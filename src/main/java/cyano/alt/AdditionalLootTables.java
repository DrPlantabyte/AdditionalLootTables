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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cyano.alt.examples.Examples;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Mod(modid = AdditionalLootTables.MODID, version = AdditionalLootTables.VERSION, name= AdditionalLootTables.NAME)
public class AdditionalLootTables
{
	public static final String NAME = "Additional Loot Tables";
	public static final String MODID = "alt";
	public static final String VERSION = "1.01";

	public static boolean enabled = true;
	public static boolean strict_mode = false;

	public static final String loot_folder_name = "additional-loot-tables";
	public static Path loot_folder = null;
	public static final Map<String,Map<String,List<LootPool>>> additional_loot = new HashMap<>(); // <category,<table name,list of jsons>> ex: <"chests",<"igloo_chest",list>>
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{

		// load config
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		enabled = config.getBoolean("enable", "Additional Loot Tables", enabled,
				"If true, then this mod will look in the config/additional-loot-tables folder for loot_table json files and merge \n" +
						"them with the existing loot tables");
		strict_mode = config.getBoolean("strict", "Additional Loot Tables", strict_mode,
				"If true, then any errors while parsing/loading loot tables will crash the game. If false, then there will be an \n" +
						"error message in the log but no crash.");

		config.save();

		loot_folder = Paths.get(event.getSuggestedConfigurationFile().getParent(),loot_folder_name);
		try {
			if (Files.notExists(loot_folder)) {
				Files.createDirectory(loot_folder);
				Examples.makeChestExample(loot_folder);
				Examples.makeEntityExample(loot_folder);
			}
		}catch(IOException ex){
			FMLLog.log(Level.ERROR,ex,"%s: Failed to extract example additional loot tables",MODID);
			if(AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
		}
	}
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		// register event handler
		MinecraftForge.EVENT_BUS.register(new WorldLoadEventHandler());
	}
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		FMLLog.info("%s: Parsing additional loot tables",MODID);
		final Gson gsonObjectConstructor = (new GsonBuilder())
				.registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer())
				.registerTypeAdapter(LootPool.class, new net.minecraft.world.storage.loot.LootPool.Serializer())
				.registerTypeAdapter(LootTable.class, new net.minecraft.world.storage.loot.LootTable.Serializer())
				.registerTypeHierarchyAdapter(LootEntry.class, new net.minecraft.world.storage.loot.LootEntry.Serializer())
				.registerTypeHierarchyAdapter(LootFunction.class, new net.minecraft.world.storage.loot.functions.LootFunctionManager.Serializer())
				.registerTypeHierarchyAdapter(LootCondition.class, new net.minecraft.world.storage.loot.conditions.LootConditionManager.Serializer())
				.registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new net.minecraft.world.storage.loot.LootContext.EntityTarget.Serializer())
				.create();
		try {
			Files.list(loot_folder).filter((Path f)->Files.isDirectory(f))
					.forEach((Path domain)->{
						try {
							List<Path> types = Files.list(domain).filter((Path f) -> Files.isDirectory(f)).collect(Collectors.toList());
							for(Path typeDir : types){
								String category = typeDir.getFileName().toString();
								List<Path> files = Files.list(typeDir).filter((Path f) -> Files.isRegularFile(f) && f.toString().toLowerCase(Locale.US).endsWith(".json")).collect(Collectors.toList());
								for(Path file : files){
									String entry = file.getFileName().toString().toLowerCase(Locale.US).replace(".json","");
									final StringBuilder json = new StringBuilder();
									Files.readAllLines(file, Charset.forName("UTF-8")).stream().forEach((String ln)->json.append(ln).append("\n"));

									LootTable table = (LootTable)gsonObjectConstructor.fromJson(json.toString(), LootTable.class);

									// if we made it this far, the json file is clean
									FMLLog.info("%s: Adding additional loot table %s/%s/%s",MODID,domain.getFileName(),category,entry);
									additional_loot
											.computeIfAbsent(category,(String key)->new HashMap<>())
											.computeIfAbsent(entry,(String key)->new ArrayList<>())
											.addAll(Arrays.asList(getPoolsFromLootTable(table)));
								}
							}
						} catch(IOException | IllegalAccessException | NoSuchFieldException ex){
							FMLLog.log(Level.ERROR,ex,"%s: Error parsing loot file.",MODID);
							if(AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
						}
					});
		} catch (IOException ex) {
			FMLLog.log(Level.ERROR,ex,"%s: Cannot access additioanl loot tables folder!",MODID);
			if(AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
		}
		//
	}

	public static LootPool[] getPoolsFromLootTable(LootTable t) throws IllegalAccessException, NoSuchFieldException {
		// time for black magic
		Field[] vars = LootTable.class.getDeclaredFields();
		for(Field v : vars){
			if(v.getType().isArray() && v.getType().getComponentType().isAssignableFrom(LootPool.class)){
				v.setAccessible(true);
				return (LootPool[])v.get(t);
			}
		}
		throw new NoSuchFieldException("Could not find LootPool[] field in LootTable");
	}

	public static void addPoolsToLootTable(LootTable t, List<LootPool> pools) throws IllegalAccessException, NoSuchFieldException {
		LootPool[] oldArray = getPoolsFromLootTable(t);
		List<LootPool> newPools = new ArrayList<>();
		newPools.addAll(Arrays.asList(oldArray));
		newPools.addAll(pools);
		LootPool[] newArray = newPools.toArray(new LootPool[newPools.size()]);

		// time for black magic
		Field[] vars = LootTable.class.getDeclaredFields();
		for(Field v : vars){
			if(v.getType().isArray() && v.getType().getComponentType().isAssignableFrom(LootPool.class)){
				v.setAccessible(true);
				Field modField = Field.class.getDeclaredField("modifiers");
				modField.setAccessible(true);
				modField.setInt(v, v.getModifiers() & ~Modifier.FINAL);
				v.set(t,newArray);
				break;
			}
		}
	}

}
