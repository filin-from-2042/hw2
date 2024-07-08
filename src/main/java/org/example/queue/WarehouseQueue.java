package org.example.queue;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WarehouseQueue<T> implements Queue<T> {

    private Node head;
    private Node tail;
    private final Lock enqLock;
    private final Lock deqLock;

    public WarehouseQueue() {
        head = new Node(null);
        tail = head;
        enqLock = new ReentrantLock();
        deqLock = new ReentrantLock();
    }

    @Override
    public void enq(T item) {
        enqLock.lock();
        try{
            Node newNode = new Node(item);
            tail.setNext(newNode);
            tail = newNode;
        } finally {
            enqLock.unlock();
        }
    }

    @Override
    public T deq() {
        deqLock.lock();
        try{
            if(head.getNext() == null){
                return null;
            }
            Node resultNode = head.getNext();
            head = head.getNext();
            return resultNode.getValue();
        } finally {
            deqLock.unlock();
        }
    }

    private class Node{
        private final T value;
        volatile private Node next;

        public Node(T value) {
            this.value = value;
            this.next = null;
        }

        public T getValue() {
            return value;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next){
            this.next = next;
        }
    }
}
