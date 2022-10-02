package com.ishland.vmp.common.general.spawn_density_cap;

import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.SpawnGroup;

public class SpawnDensityCapperDensityCapDelegate {

    public static Object2IntMap<SpawnGroup> delegateSpawnGroupDensities(int[] spawnGroupDensities) {
        return new AbstractObject2IntMap<>() {
            @Override
            public int size() {
                return spawnGroupDensities.length;
            }

            @Override
            public ObjectSet<Entry<SpawnGroup>> object2IntEntrySet() {
                return new AbstractObjectSet<>() {
                    @Override
                    public ObjectIterator<Entry<SpawnGroup>> iterator() {
                        return new AbstractObjectIterator<>() {
                            private int index = 0;

                            @Override
                            public boolean hasNext() {
                                return index < spawnGroupDensities.length;
                            }

                            @Override
                            public Entry<SpawnGroup> next() {
                                final int index = this.index;
                                this.index++;
                                return new AbstractObject2IntMap.BasicEntry<>(
                                        SpawnGroup.values()[index], spawnGroupDensities[index]);
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return spawnGroupDensities.length;
                    }
                };
            }

            @Override
            public int getInt(Object key) {
                return spawnGroupDensities[((SpawnGroup) key).ordinal()];
            }
        };
    }

}
