package com.grandsages;

import static org.junit.Assert.assertTrue;

import com.grandsages.utils.CircleNode;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        var node = new CircleNode<Integer>(10);
        node.add(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20});
        assertTrue(node.at(6).getData() == 7);
        assertTrue(node.at(19).getData() == 20);
        node.removeAt(10);
        assertTrue(node.at(10).getData() == 12);
    }
}
