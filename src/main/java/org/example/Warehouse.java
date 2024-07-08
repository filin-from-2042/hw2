package org.example;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.example.queue.WarehouseQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * В данной реализации, выгрузкой/загрузкой грузовиков занимается поток скалада. По этой причине, в качестве механизма получения прибывшего грузовика
 * выбрал очередь UnboundedQueue. LockFree реализация, на мой взгляд, при таком варианте разгрузки была бы излишним усложнением.
 * Так же, из-за того, что каждый поток склада работает исключительно со своим хранилищем блоков при выгрузке/загрузке, то конкурентого доступа
 * не происходит и синхронизация при работе с коллекцией storage не требуется.
 */

@Log4j2
@Getter
public class Warehouse extends Thread {

    private final WarehouseQueue<Truck> truckQueue = new WarehouseQueue<>();
    private final List<Block> storage = new ArrayList<>();

    public Warehouse(String name) {
        super(name);
    }

    public Warehouse(String name, Collection<Block> initialStorage) {
        this(name);
        storage.addAll(initialStorage);
    }

    @Override
    public void run() {
        Truck truck;
        while (!currentThread().isInterrupted()) {
            truck = getNextArrivedTruck();
            if (truck == null) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    if (currentThread().isInterrupted()) {

                        break;
                    }
                }
                continue;
            }
            if (truck.getBlocks().isEmpty()) {
                loadTruck(truck);
            } else {
                unloadTruck(truck);
            }
            synchronized (truck) {
                // если будить с помощью notify(), то почему-то единственный поток Truck 0 не просыпается и зависает в WAIT
                truck.notifyAll();
            }
        }
        log.info("Warehouse thread interrupted");

    }

    private void loadTruck(Truck truck) {
        log.info("Loading truck {}", truck.getName());
        Collection<Block> blocksToLoad = getFreeBlocks(truck.getCapacity());
        try {
            sleep(10L * blocksToLoad.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while loading truck", e);
        }
        truck.getBlocks().addAll(blocksToLoad);
        log.info("Truck loaded {}", truck.getName());
    }

    private Collection<Block> getFreeBlocks(int maxItems) {
        List<Block> freeBlocks = new ArrayList<>(storage.subList(0, Math.min(maxItems, storage.size())));
        storage.removeAll(freeBlocks);

        return freeBlocks;
    }

    private void returnBlocksToStorage(List<Block> returnedBlocks) {
        storage.addAll(returnedBlocks);
    }

    private void unloadTruck(Truck truck) {
        log.info("Unloading truck {}", truck.getName());
        List<Block> arrivedBlocks = truck.getBlocks();
        try {
            sleep(100L * arrivedBlocks.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while unloading truck", e);
        }
        returnBlocksToStorage(arrivedBlocks);
        truck.getBlocks().clear();
        log.info("Truck unloaded {}", truck.getName());
    }

    private Truck getNextArrivedTruck() {
        return truckQueue.deq();
    }


    public void arrive(Truck truck) {
        truckQueue.enq(truck);
        synchronized (truck) {
            try {
                truck.wait();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
