package com.bishe.service.impl;

import com.bishe.entity.ChainTransaction;
import com.bishe.mapper.ChainTransactionMapper;
import com.bishe.service.IChainTransactionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChainTransactionServiceImpl extends ServiceImpl<ChainTransactionMapper, ChainTransaction> implements IChainTransactionService {

    @Value("${bishe.chain.enabled:true}")
    private boolean enabled;

    @Value("${bishe.chain.rpc-url:http://127.0.0.1:8545}")
    private String rpcUrl;

    @Value("${bishe.chain.from-address:0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266}")
    private String fromAddress;

    @Value("${bishe.chain.contract-address:}")
    private String contractAddress;

    @Value("${bishe.chain.deployment-file:../hardhat/deployments/localhost.json}")
    private String deploymentFile;

    @Value("${bishe.chain.gas-price:1000000000}")
    private BigInteger gasPrice;

    @Value("${bishe.chain.gas-limit:300000}")
    private BigInteger gasLimit;

    @Value("${bishe.chain.receipt-poll-interval-ms:1000}")
    private long receiptPollIntervalMs;

    @Value("${bishe.chain.receipt-poll-attempts:60}")
    private int receiptPollAttempts;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String submitTransaction(String methodName, String params) {
        String method = String.valueOf(methodName == null ? "" : methodName).trim();
        String p = params == null ? "" : params;
        String resolvedContract = resolveContractAddress();
        if (!enabled || resolvedContract == null) {
            return submitMock(method, p);
        }

        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        Long chainId = null;
        try {
            chainId = web3j.ethChainId().send().getChainId().longValue();
        } catch (Exception ignored) {
        }

        byte[] hashBytes = Hash.sha3(p.getBytes(StandardCharsets.UTF_8));
        Bytes32 paramsHash = new Bytes32(hashBytes);
        List<org.web3j.abi.datatypes.Type> inputs = List.of(new Utf8String(method), paramsHash);
        Function function = new Function("recordEvidence", inputs, Collections.emptyList());
        String data = FunctionEncoder.encode(function);

        TransactionManager txManager = new ClientTransactionManager(web3j, fromAddress);
        String txHash;
        Long blockNumber = null;
        try {
            EthSendTransaction sent = txManager.sendTransaction(gasPrice, gasLimit, resolvedContract, data, BigInteger.ZERO);
            if (sent.hasError()) {
                String mock = submitFailedChainRecord(chainId, resolvedContract, method, p, sent.getError().getMessage());
                return mock;
            }
            txHash = sent.getTransactionHash();
            TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, receiptPollIntervalMs, receiptPollAttempts);
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);
            if (receipt != null && receipt.getBlockNumber() != null) {
                blockNumber = receipt.getBlockNumber().longValue();
            }
            ChainTransaction tx = new ChainTransaction();
            tx.setTxHash(txHash);
            tx.setBlockHeight(blockNumber);
            tx.setChainId(chainId);
            tx.setFromAddress(normalizeHexAddress(fromAddress));
            tx.setContractAddr(normalizeHexAddress(resolvedContract));
            tx.setMethodName(method);
            tx.setParams(p);
            tx.setStatus(0);
            tx.setCreateTime(LocalDateTime.now());
            this.save(tx);
            return txHash;
        } catch (Exception e) {
            return submitFailedChainRecord(chainId, resolvedContract, method, p, e.getMessage());
        } finally {
            try {
                web3j.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    private String submitFailedChainRecord(Long chainId, String resolvedContract, String method, String params, String errorMessage) {
        String txHash = "0x" + Hash.sha3String(method + params + System.currentTimeMillis()).substring(2, 66);
        ChainTransaction tx = new ChainTransaction();
        tx.setTxHash(txHash);
        tx.setBlockHeight(null);
        tx.setChainId(chainId);
        tx.setFromAddress(normalizeHexAddress(fromAddress));
        tx.setContractAddr(normalizeHexAddress(resolvedContract));
        tx.setMethodName(method);
        tx.setParams(params);
        tx.setStatus(1);
        tx.setErrorMessage(errorMessage);
        tx.setCreateTime(LocalDateTime.now());
        this.save(tx);
        return txHash;
    }

    private String submitMock(String method, String params) {
        String txHash = "0x" + Hash.sha3String(method + params + System.currentTimeMillis()).substring(2, 66);
        ChainTransaction tx = new ChainTransaction();
        tx.setTxHash(txHash);
        tx.setBlockHeight(null);
        tx.setChainId(null);
        tx.setFromAddress(normalizeHexAddress(fromAddress));
        tx.setContractAddr(null);
        tx.setMethodName(method);
        tx.setParams(params);
        tx.setStatus(0);
        tx.setCreateTime(LocalDateTime.now());
        this.save(tx);
        return txHash;
    }

    private String resolveContractAddress() {
        String direct = normalizeHexAddress(contractAddress);
        if (direct != null) {
            return direct;
        }
        Path p = resolveDeploymentPath(deploymentFile);
        if (p == null) {
            return null;
        }
        try {
            if (!Files.exists(p)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(Files.readString(p, StandardCharsets.UTF_8));
            String addr = root.path("evidenceRegistry").asText(null);
            return normalizeHexAddress(addr);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path resolveDeploymentPath(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        Path p = Paths.get(text);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }

    private String normalizeHexAddress(String addr) {
        if (addr == null) return null;
        String t = addr.trim();
        if (t.isEmpty()) return null;
        if (!t.startsWith("0x") && !t.startsWith("0X")) {
            t = "0x" + t;
        }
        try {
            String clean = Numeric.cleanHexPrefix(t);
            if (clean.length() != 40) {
                return null;
            }
            return "0x" + clean;
        } catch (Exception e) {
            return null;
        }
    }
}
