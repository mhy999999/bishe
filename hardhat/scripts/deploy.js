const fs = require('fs');
const path = require('path');

function compileEvidenceRegistry() {
  const solc = require('solc');
  const sourcePath = path.join(__dirname, '..', 'contracts', 'EvidenceRegistry.sol');
  const source = fs.readFileSync(sourcePath, 'utf-8');
  const input = {
    language: 'Solidity',
    sources: {
      'EvidenceRegistry.sol': { content: source }
    },
    settings: {
      outputSelection: {
        '*': {
          '*': ['abi', 'evm.bytecode.object']
        }
      }
    }
  };

  const output = JSON.parse(solc.compile(JSON.stringify(input)));
  const errors = Array.isArray(output.errors) ? output.errors : [];
  const fatal = errors.filter((e) => (e.severity || '').toLowerCase() === 'error');
  if (fatal.length) {
    throw new Error(fatal.map((e) => e.formattedMessage || e.message).join('\n'));
  }

  const contract = output.contracts['EvidenceRegistry.sol']['EvidenceRegistry'];
  const abi = contract.abi;
  const bytecode = '0x' + contract.evm.bytecode.object;
  return { abi, bytecode };
}

async function main() {
  const { ethers } = require('ethers');
  const { abi, bytecode } = compileEvidenceRegistry();

  const provider = new ethers.JsonRpcProvider('http://127.0.0.1:8545');
  const signer = await provider.getSigner(0);
  const factory = new ethers.ContractFactory(abi, bytecode, signer);
  const evidenceRegistry = await factory.deploy();
  await evidenceRegistry.waitForDeployment();
  const address = await evidenceRegistry.getAddress();

  const outDir = path.join(__dirname, '..', 'deployments');
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, 'localhost.json');
  fs.writeFileSync(outFile, JSON.stringify({ evidenceRegistry: address }, null, 2), 'utf-8');

  console.log('evidenceRegistry:', address);
  console.log('deployment file:', outFile);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
