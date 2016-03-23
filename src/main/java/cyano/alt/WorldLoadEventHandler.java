/*
Copyright (c) 2016 Christopher C. Hall

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package cyano.alt;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Level;

import static cyano.alt.AdditionalLootTables.MODID;


/**
 * Created by Chris on 3/21/2016.
 */
public class WorldLoadEventHandler {

	static LootTableManager reference = null;

	@SubscribeEvent(priority= EventPriority.NORMAL)
	public void onWorldLoad(WorldEvent.Load event){
		// merge loot table pools
		if(event.world == null || event.world.isRemote) return;
		LootTableManager lootManager = event.world.getLootTableManager();
		if (lootManager == null) {
			FMLLog.severe("%s: LootTableMAnager is null!", MODID);
		}
		synchronized (lootManager) { // in case of multi-threaded parallel dimension loading
			if(lootManager == reference) return; // already loaded this manager (every dimension will fire this event and they don't have their dimension IDs yet
			reference = lootManager;
			for (String category : AdditionalLootTables.additional_loot.keySet()) {
				for (String entry : AdditionalLootTables.additional_loot.get(category).keySet()) {
					ResourceLocation rsrc = new ResourceLocation(category + "/" + entry);
					LootTable lootTable = lootManager.getLootTableFromLocation(rsrc);
					FMLLog.info("%s: adding more loot to loot table %s", MODID, rsrc);
					try {
						AdditionalLootTables.addPoolsToLootTable(lootTable, AdditionalLootTables.additional_loot.get(category).get(entry));
					} catch (IllegalAccessException | NoSuchFieldException ex) {
						FMLLog.log(Level.ERROR, ex, "%s: Failed to add additional loot to table %s", MODID, rsrc);
						if(AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
					}
				}
			}
		}
	}
}
