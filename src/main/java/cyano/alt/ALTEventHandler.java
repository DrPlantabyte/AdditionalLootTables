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

import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Map;

import static cyano.alt.AdditionalLootTables.MODID;


/**
 * Created by Chris on 3/21/2016.
 */
public class ALTEventHandler {

	@SubscribeEvent(priority= EventPriority.NORMAL)
	public void onLootLoad(LootTableLoadEvent event){
		if(event.getName().getResourceDomain().equals("minecraft") == false) return; // loot table from another mod (too fancy for ALT)
		String categoryAndEntry = event.getName().getResourcePath(); // e.g. "chests/abandoned_mineshaft"
		if(categoryAndEntry.contains("/") == false) return; // not valid
		// category is "chests" or "entities"
		String category = categoryAndEntry.substring(0,categoryAndEntry.indexOf('/'));
		// entry is the name of the loot table (e.g. "abandoned_mineshaft")
		String entry = categoryAndEntry.substring(categoryAndEntry.indexOf('/')+1,categoryAndEntry.length());
		final Map<String, Map<String, List<LootPool>>> additional_loot = AdditionalLootTables.getAdditionalLootTables();
		if(additional_loot.containsKey(category)
				&& additional_loot.get(category).containsKey(entry)){
			List<LootPool> pools = additional_loot.get(category).get(entry);
			if(pools == null || pools.isEmpty()) return; // nothing to add
			if(event.getTable() == null) {
				// table was removed by another mod
				FMLLog.info("%s: creating new loot table %s", MODID, event.getName());
				event.setTable(new LootTable(pools.toArray(new LootPool[pools.size()])));
			} else {
				// table exists, add pools to it
				FMLLog.info("%s: adding more loot to loot table %s", MODID, event.getName());
				for (LootPool pool : pools) {
					event.getTable().addPool(pool);
				}
			}
		}
	}
}
