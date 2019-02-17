package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class MerkleTreeTest {

    @Test
    public void testSignatureVerification() {
        String seed = Trytes.randomSequenceOfLength(81);
        int depth = 3;
        MerkleTree merkleTree = new MerkleTree(seed, 3);

        String toSign = Trytes.randomSequenceOfLength(27);
        int randomIndex = (int)(Math.random()*Math.pow(2, depth));
        MerkleTree.Signature signature = merkleTree.sign(randomIndex, toSign);

        String addressOfMerkleTree = merkleTree.getAddress();
        String addressOfSignature = signature.deriveAddress();

        Assert.assertEquals("Signature validation failed.", addressOfMerkleTree, addressOfSignature);
    }
}