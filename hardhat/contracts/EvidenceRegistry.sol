pragma solidity ^0.8.20;

contract EvidenceRegistry {
    event EvidenceRecorded(address indexed sender, string methodName, bytes32 paramsHash, uint256 timestamp);

    function recordEvidence(string calldata methodName, bytes32 paramsHash) external {
        emit EvidenceRecorded(msg.sender, methodName, paramsHash, block.timestamp);
    }
}
