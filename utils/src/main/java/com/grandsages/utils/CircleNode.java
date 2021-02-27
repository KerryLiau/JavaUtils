package com.grandsages.utils;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 環狀結點
 */
public class CircleNode<T> {

    private int INDEX_INTERVAL = 1000;
    private final AtomicInteger size = new AtomicInteger(0);
    private Object[] index = new Object[1];
    private int nextIndex = 0;
    private long modifyCount = 0;
    /**
     * 第一個結點
     */
    private DataNode<T> head;

    public CircleNode() {}

    public CircleNode(int indexInterval) {
        INDEX_INTERVAL = indexInterval;
    }



    /*
     * ---------------
     * ---------------
     * Public Methods
     * ---------------
     * ---------------
     */

    @SafeVarargs
    public static <T> CircleNode<T> createByValues(T... dataArray) {
        CircleNode<T> result = new CircleNode<>();
        for (T data : dataArray) add(result, data);
        return result;
    }

    @SafeVarargs
    public final void add(T... dataArray) {
        for (var data : dataArray)
            add(this, data);
    }

    public void addAfter(Integer specificID, T data) {
        if (specificID > getLastPosition()) {
            add(data);
            return;
        }
        DataNode<T> current, next, newNode;
        current = findSpecificNode(specificID);
        if (current == null) {
            add(data);
            return;
        }
        next = current.next;
        newNode = new DataNode<>();
        newNode.data = data;
        // linking
        current.next = newNode;
        newNode.prev = current;
        newNode.next = next;
        next.prev = newNode;
        size.getAndIncrement();
        checkIndex(newNode);
        recordOperatedCount();
    }

    public void removeLast() {
        int size = getSize();
        if (size == 0) return;
        DataNode<T> tail, tailPrev;
        tail = getTailNode();
        tailPrev = tail.getPrev();
        tailPrev.next = head;
        head.prev = tailPrev;
        this.size.getAndDecrement();
        adjustIndexAfter(getLastPosition());// last position
        recordOperatedCount();
    }

    public void removeAt(Integer position) {
        if (getSize() == position + 1) {
            removeLast();
            return;
        }
        DataNode<T> current, prev, next;
        current = findSpecificNode(position);
        if (current == null) return;
        prev = current.getPrev();
        next = current.getNext();
        prev.next = next;
        next.prev = prev;
        size.getAndDecrement();
        adjustIndexAfter(position);
        recordOperatedCount();
    }

    public void forEachNext(NodeFunction<T> function) {
        DataNode<T> node = goHead();
        var count = modifyCount;
        var time = getSize();
        while (time-- > 0) {
            function.invoke(node.data);
            node = node.next;
            if (count != modifyCount) throwConcurrentException();
        }
    }

    public void forEachPrev(NodeFunction<T> function) {
        DataNode<T> node = goHead();
        var count = modifyCount;
        var time = getSize();
        while (time-- > 0) {
            function.invoke(node.data);
            node = node.prev;
            if (count != modifyCount) throwConcurrentException();
        }
    }

    public void forEachNextIn(Integer time, NodeFunction<T> function) {
        DataNode<T> node = goHead();
        var count = modifyCount;
        while (time-- > 0) {
            function.invoke(node.data);
            node = node.next;
            if (count != modifyCount) throwConcurrentException();
        }
    }

    public void forEachPrevIn(Integer time, NodeFunction<T> function) {
        DataNode<T> node = goHead();
        var count = modifyCount;
        while (time-- > 0) {
            function.invoke(node.data);
            node = node.prev;
            if (count != modifyCount) throwConcurrentException();
        }
    }

    public DataNode<T> at(Integer specificID) {
        return findSpecificNode(specificID);
    }

    public int getSize() {
        return this.size.get();
    }

    @Override
    public String toString() {
        var resultBuilder = new StringBuilder();
        resultBuilder.append("{ ");
        forEachNext(data -> resultBuilder.append(data).append(", "));
        resultBuilder.deleteCharAt(resultBuilder.lastIndexOf(", "));
        resultBuilder.append(" }");
        return resultBuilder.toString();
    }



    /*
     * ----------------
     * ----------------
     * Private Methods
     * ----------------
     * ----------------
     */

    private DataNode<T> goHead() {
        return head;
    }

    private int getLastPosition() {
        return getSize() - 1;
    }

    private void adjustIndexAfter(int position) {
        if (position == 20) {
            System.out.println();
        }
        // all index node position will - 1 after remove operation
        var next = (position / INDEX_INTERVAL);
        if (next < 0) return;
        while (next < index.length) {
            var indexNode = (DataNode<T>) index[next];
            if (indexNode == null) break;
            var nextNode = position == getLastPosition()
                ? null
                : indexNode.next.equals(head)
                ? null
                : indexNode.next;
            index[next] = nextNode;
            next++;
        }
    }

    private DataNode<T> findIndex(int position) {
        return position >= index.length
            ? null
            : (DataNode<T>) index[position];
    }

    private void checkIndex(DataNode<T> node) {
        var size = getSize();
        if (size != 0 && size % INDEX_INTERVAL == 0) addIndex(node);
    }

    private void addIndex(DataNode<T> node) {
        if (nextIndex + 1 > index.length) growIndexSize();
        index[nextIndex] = node;
        nextIndex++;
    }

    private void growIndexSize() {
        var oldSize = index.length;
        var newSize = oldSize + (oldSize >> 1);
        if (newSize == 1) newSize++;
        index = Arrays.copyOf(index, newSize);
    }

    /**
     * search DataNode at modePosition,
     * for speed up search time, use index
     * @param nodePosition 指定的結點 id
     * @return nodePosition 對應到的結點
     */
    private DataNode<T> findSpecificNode(Integer nodePosition) {
        var size = getSize();
        if (nodePosition == null || nodePosition > size - 1) return null;
        var beginNode = head;
        if (nodePosition < INDEX_INTERVAL - 1) return findByClockWise(beginNode, nodePosition);
        /*
         * if nodePosition greater than first index position, let index instead of head
         */
        var realPosition = nodePosition + 1;// use nodePosition + 1 to turn index to real position
        var indexPosition = realPosition / INDEX_INTERVAL - 1;
        beginNode = findIndex(indexPosition);
        var distanceFromCurrentIndex = realPosition % INDEX_INTERVAL;
        if (distanceFromCurrentIndex == 0) return beginNode;
        /*
         * if index not the specific node, use index to be iterator
         */
        var nextIndex = findIndex(indexPosition + 1);
        var halfIndexInterval = INDEX_INTERVAL >> 1;
        var distanceFromNextIndex = distanceFromCurrentIndex - halfIndexInterval;
        var isCloserFromNextIndex = distanceFromNextIndex > 0;
        /*
         * make sure use first index or next index, for best performance
         */
        if (nextIndex != null && isCloserFromNextIndex)
            return findByCounterClockWise(nextIndex, halfIndexInterval - distanceFromNextIndex);
        else
            return findByClockWise(beginNode, distanceFromCurrentIndex);
    }

    private DataNode<T> findByClockWise(DataNode<T> index, Integer dist) {
        if (dist == 0) return index;
        DataNode<T> currentNode = index;
        while (dist-- > 0) currentNode = currentNode.next;
        return currentNode;
    }

    private DataNode<T> findByCounterClockWise(DataNode<T> index, Integer dist) {
        int distance = dist;
        DataNode<T> currentNode = index;
        while (distance-- > 0) currentNode = currentNode.prev;
        return currentNode;
    }

    private static <T> void add(CircleNode<T> node, T data) {
        // 建立新節點 next
        DataNode<T> head, last, next;
        head = node.head;
        last = head == null ? null : node.getTailNode();
        next = new DataNode<>();
        next.data = data;
        if (head == null) {
            // 初始化第一個節點
            head = next;
            node.head = head;
        } else {
            if (head.getNext() == null) {
                // 第一個節點需接上
                head.next = next;
                head.prev = next;
                next.prev = head;
            } else {
                // 接合 last 跟 next
                if (last == null) last = next;
                else last.next = next;
                next.prev = last;
                next.next = head;
            }
            // 將結點串成環狀
            last = next;
            last.next = head;
            head.prev = last;
        }
        node.size.getAndIncrement();
        node.checkIndex(next);
        node.recordOperatedCount();
    }

    private DataNode<T> getTailNode() {
        return head.getPrev();
    }

    private void recordOperatedCount() {
        modifyCount++;
    }

    private void throwConcurrentException() {
        throw new ConcurrentModificationException("Can not mutate node while iterating.");
    }

    /**
     * Read Only Data Node
     * @param <T> Data Type
     */
    public static class DataNode<T> {
        private T data;
        /**
         * 順時針下一個結點
         */
        private DataNode<T> next;
        /**
         * 逆時針下一個結點
         */
        private DataNode<T> prev;

        public T getData() {
            return data;
        }

        public DataNode<T> getNext() {
            return next;
        }

        public DataNode<T> getPrev() {
            return prev;
        }

        @Override
        public String toString() {
            return String.format("{data:%s}", data);
        }
    }
}
