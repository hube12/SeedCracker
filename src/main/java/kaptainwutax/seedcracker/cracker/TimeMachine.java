package kaptainwutax.seedcracker.cracker;

import kaptainwutax.seedcracker.SeedCracker;
import kaptainwutax.seedcracker.cracker.population.PopulationData;
import kaptainwutax.seedcracker.util.Log;
import kaptainwutax.seedcracker.util.Rand;
import kaptainwutax.seedcracker.util.math.LCG;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeMachine {

    private LCG inverseLCG = Rand.JAVA_LCG.combine(-2);
    public int THREAD_COUNT = 4;
    public ExecutorService SERVICE = Executors.newFixedThreadPool(THREAD_COUNT);

    private boolean isRunning = false;

    public TimeMachine() {

    }

    public List<Long> bruteforceRegion(int pillarSeed, int region, long size, List<StructureData> structureDataList, List<DecoratorData> decoratorDataList) {
        List<Long> result = new ArrayList<>();
        Rand rand = new Rand(0L);

        for(long i = 0; i < (1L << 32); i++) {
            if((i & ((1L << 28) - 1)) == 0) {
                Log.warn("Progress " + (i * 100.0f) / (1L << 32) + "%...");
            }

        for(long i = start; i < end; i++) {
            long structureSeed = this.timeMachine(i, pillarSeed);
            boolean goodSeed = true;

            for(StructureData structureData: structureDataList) {
                if(!goodSeed)break;
                if(!structureData.test(structureSeed, rand))goodSeed = false;
            }

            for(DecoratorData decoratorData : decoratorDataList) {
                if(!goodSeed)break;
                if(!decoratorData.test(structureSeed))goodSeed = false;
            }

            if(goodSeed) {
                result.add(structureSeed);
            }
        }

        return result;
    }

    public Set<Long> buildStructureSeeds(int pillarSeed, List<StructureData> structureDataList, List<DecoratorData> decoratorDataList, Set<Long> structureSeeds) {
        if(this.isRunning) {
            throw new IllegalStateException("Time Machine is already running");
        }

        this.isRunning = true;
        long size = (long)Math.ceil((double)(1L << 32) / THREAD_COUNT);
        AtomicInteger progress = new AtomicInteger();

        for(int i = 0; i < THREAD_COUNT; i++) {
            int finalI = i;

            SERVICE.submit(() -> {
                structureSeeds.addAll(this.bruteforceRegion(pillarSeed, finalI, size, structureDataList, decoratorDataList));
                Log.warn("Completed thread " + finalI + "!");
                progress.getAndIncrement();
            });
        }

        while(progress.get() < THREAD_COUNT) {
            try {Thread.sleep(20);}
            catch(InterruptedException e) {e.printStackTrace();}
        }

        this.isRunning = false;
        return structureSeeds;
    }

    public long timeMachine(long partialWorldSeed, int pillarSeed) {
        long currentSeed = 0L;
        currentSeed |= (partialWorldSeed & 0xFFFF0000L) << 16;
        currentSeed |= (long)pillarSeed << 16;
        currentSeed |= partialWorldSeed & 0xFFFFL;

        currentSeed = this.inverseLCG.nextSeed(currentSeed);
        currentSeed ^= Rand.JAVA_LCG.multiplier;
        return currentSeed;
    }

    public void stop() {
        this.isRunning = false;
    }

}
