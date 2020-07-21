/**
 * Copyright 2019 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.jvm;

import io.micrometer.core.lang.Nullable;

import java.lang.management.*;
import java.util.Optional;
import java.util.function.ToLongFunction;

class JvmMemory {

    private JvmMemory() {
    }

    static Optional<MemoryPoolMXBean> getOldGen() {
        return ManagementFactory
                .getPlatformMXBeans(MemoryPoolMXBean.class)
                .stream()
                .filter(JvmMemory::isHeap)
                .filter(mem -> isOldGenPool(mem.getName()))
                .findAny();
    }

    static boolean isConcurrentPhase(String cause) {
        return "No GC".equals(cause);
    }

    static boolean isYoungGenPool(String name) {
        return name.endsWith("Eden Space");
    }

    static boolean isOldGenPool(String name) {
        return name.endsWith("Old Gen") || name.endsWith("Tenured Gen");
    }

    private static boolean isHeap(MemoryPoolMXBean memoryPoolBean) {
        return MemoryType.HEAP.equals(memoryPoolBean.getType());
    }

    static double getUsageValue(MemoryPoolMXBean memoryPoolMXBean, ToLongFunction<MemoryUsage> getter) {
        MemoryUsage usage = getUsage(memoryPoolMXBean);
        if (usage == null) {
            return Double.NaN;
        }
        return getter.applyAsLong(usage);
    }

    @Nullable
    private static MemoryUsage getUsage(MemoryPoolMXBean memoryPoolMXBean) {
        try {
            return memoryPoolMXBean.getUsage();
        } catch (InternalError e) {
            // Defensive for potential InternalError with some specific JVM options. Based on its Javadoc,
            // MemoryPoolMXBean.getUsage() should return null, not throwing InternalError, so it seems to be a JVM bug.
            return null;
        }
    }

    static double getTotalAreaUsageValue(MemoryMXBean memoryMXBean, ToLongFunction<MemoryUsage> getter, String area) {
        MemoryUsage usage = getTotalAreaUsage(memoryMXBean, area);
        if (usage == null) {
            return Double.NaN;
        }
        return getter.applyAsLong(usage);
    }

    @Nullable
    private static MemoryUsage getTotalAreaUsage(MemoryMXBean memoryMXBean, String area) {
        MemoryUsage usage = null;
        try {
            if(area.equals("heap")){
                usage = memoryMXBean.getHeapMemoryUsage();
            }
            else if(area.equals("nonheap")){
                usage = memoryMXBean.getNonHeapMemoryUsage();
            }
            return usage;
        } catch (InternalError e) {
            // Defensive for potential InternalError with some specific JVM options. Based on its Javadoc,
            // MemoryPoolMXBean.getUsage() should return null, not throwing InternalError, so it seems to be a JVM bug.
            return null;
        }
    }

    static double getHeapUsagePercent(MemoryMXBean memoryMXBean){
        MemoryUsage usage = getTotalAreaUsage(memoryMXBean, "heap");
        if (usage == null) {
            return Double.NaN;
        }
        return ((double)usage.getUsed()/usage.getMax()) * 100;
    }


}
