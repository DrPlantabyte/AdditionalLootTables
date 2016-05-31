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

import com.google.common.collect.Queues;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import cyano.alt.examples.Examples;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

@Mod(modid = AdditionalLootTables.MODID, version = AdditionalLootTables.VERSION, name= AdditionalLootTables.NAME)
public class AdditionalLootTables
{
	public static final String NAME = "Additional Loot Tables";
	public static final String MODID = "alt";
	public static final String VERSION = "1.05";

	public static boolean enabled = true;
	public static boolean strict_mode = false;

	public static final String loot_folder_name = "additional-loot-tables";
	public static Path loot_folder = null;
	private static Map<String,Map<String,List<LootPool>>> additional_loot = null; // <category,<table name,list of jsons>> ex: <"chests",<"igloo_chest",list>>
	
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
		EVENT_BUS.register(new ALTEventHandler());
	}
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		getAdditionalLootTables(); // forces parsing of files during initialization
	}

	private static final AtomicInteger hashCounter = new AtomicInteger(0);
	/**
	 * Parses the ALT loot files if they are not already cached and returns the loot tables.
	 * DO NOT INVOKE BEFORE POST-INIT PHASE!!!
	 * @return A Map of loot pools where the first level of the map is the category ("chests" or "entities") and
	 * the second level is the table name (e.g. "village_blacksmith") and the third level is a list of LootPool
	 * instances.
	 */
	public synchronized static Map<String,Map<String,List<LootPool>>> getAdditionalLootTables(){
		if(additional_loot == null) {
			additional_loot = new HashMap<>();
			FMLLog.info("%s: Parsing additional loot tables", MODID);
			final Gson gsonObjectConstructor = (new GsonBuilder())
					.registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer())
					.registerTypeAdapter(LootPool.class, new LootPool.Serializer())
					.registerTypeAdapter(LootTable.class, new LootTable.Serializer())
					.registerTypeHierarchyAdapter(LootEntry.class, new LootEntry.Serializer())
					.registerTypeHierarchyAdapter(LootFunction.class, new LootFunctionManager.Serializer())
					.registerTypeHierarchyAdapter(LootCondition.class, new LootConditionManager.Serializer())
					.registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer())
					.create();
			try {
				Files.list(loot_folder).filter((Path f) -> Files.isDirectory(f))
						.forEach((Path domain) -> {
							try {
								List<Path> types = Files.list(domain).filter((Path f) -> Files.isDirectory(f)).collect(Collectors.toList());
								for (Path typeDir : types) {
									String category = typeDir.getFileName().toString();
									List<Path> files = Files.list(typeDir).filter((Path f) -> Files.isRegularFile(f) && f.toString().toLowerCase(Locale.US).endsWith(".json")).collect(Collectors.toList());
									for (Path file : files) {
										String entry = file.getFileName().toString().toLowerCase(Locale.US).replace(".json", "");
										final StringBuilder jsonStringBuilder = new StringBuilder();
										FMLLog.info("%s: Parsing file %s", MODID, file);
										Files.readAllLines(file, Charset.forName("UTF-8")).stream().forEach((String ln) -> jsonStringBuilder.append(ln).append("\n"));

										// Hack-in name info that Forge now wants
										JsonObject jsonObject = JsonParser.object().from(new StringReader(jsonStringBuilder.toString()));
										JsonArray pools = jsonObject.getArray("pools");
										if(pools != null && !pools.isEmpty()){
											for(int i = 0; i < pools.size(); i++){
												((JsonObject)pools.get(i)).put("name",domain.getFileName()+"/"+category+"/"
														+entry+"#"+(i+1));

												// Damn you, Forge! Why do you require that every single pool and entry be unique?
												JsonArray entries = ((JsonObject) pools.get(i)).getArray("entries");
												if(entries != null){
													for(int e = 0; e < entries.size(); e++){
														String unique = "_entry_".concat(String.valueOf(hashCounter.incrementAndGet()));
														((JsonObject)entries.get(e)).put("entryName",unique);
													}
												}
											}
										}


										// TODO: check if we can remove hacking on next Forge update (does ForgeHooks.getLootTableContext() or LootTableManager.loadLootTable(...) throw an exception in the absence of a context object? And can you deserialize a LootTable without triggering events?)
										Object busCache = hackDisableEventBus();
										// Damn you, Forge! Why did you inject your hooks into the GSON PARSER?!?!
										// Now it is impossible to parse a JSON file into a LootTable without triggering an event
										pushLootTableContext(category,entry);
										LootTable table = (LootTable) gsonObjectConstructor.fromJson(
												JsonWriter.string().object(jsonObject).done(),
												LootTable.class);
										popLootTableContext();
										hackEnableEventBus(busCache);

										// if we made it this far, the json file is clean
										FMLLog.info("%s: Adding additional loot table %s/%s/%s", MODID, domain.getFileName(), category, entry);
										additional_loot
												.computeIfAbsent(category, (String key) -> new HashMap<>())
												.computeIfAbsent(entry, (String key) -> new ArrayList<>())
												.addAll(Arrays.asList(getPoolsFromLootTable(table)));
									}
								}
							} catch (IOException | com.google.gson.JsonParseException | com.grack.nanojson.JsonParserException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException | ClassNotFoundException | InstantiationException | InvocationTargetException ex) {
								FMLLog.log(Level.ERROR, ex, "%s: Error parsing loot file.", MODID);
								if (AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
							}
						});
			} catch (IOException ex) {
				FMLLog.log(Level.ERROR, ex, "%s: Cannot access additional loot tables folder!", MODID);
				if (AdditionalLootTables.strict_mode) throw new RuntimeException(ex);
			}
		}

		return additional_loot;
	}

	private static final void removeFinalModifierFromField(Field f) throws NoSuchFieldException, IllegalAccessException {
		// Warning: invoking shadow magic
		if((f.getModifiers() & Modifier.FINAL) == 0) return; // already done
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.set(f,(int)modifiers.get(f) & ~Modifier.FINAL);
	}

	private static final String className_LootTableContext = ForgeHooks.class.getCanonicalName()+"$LootTableContext";
	private static final String fieldName_lootContext = "lootContext";
	private static final String fieldName_EVENT_BUS = "EVENT_BUS";

	private static Object hackDisableEventBus() throws NoSuchFieldException, IllegalAccessException {
		Object cache = MinecraftForge.EVENT_BUS;
		Field busField = MinecraftForge.class.getDeclaredField(fieldName_EVENT_BUS);
		busField.setAccessible(true);
		removeFinalModifierFromField(busField);
		busField.set(null,new EventBus());

		return cache;
	}

	private static void hackEnableEventBus(Object cache) throws NoSuchFieldException, IllegalAccessException {
		Field busField = MinecraftForge.class.getDeclaredField(fieldName_EVENT_BUS);
		busField.setAccessible(true);
		removeFinalModifierFromField(busField);
		busField.set(null,cache);
	}

	private static void popLootTableContext() throws NoSuchFieldException, IllegalAccessException {
		ThreadLocal<Deque> contextQ = hackLootTableContextDeque();
		if(contextQ.get() != null){
			contextQ.get().pop();
		}
	}


	private static void pushLootTableContext(String category, String entry) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
		ThreadLocal<Deque> contextQ = hackLootTableContextDeque();
		if(contextQ.get() == null){
			contextQ.set(Queues.newArrayDeque());
		}
		Object ctx = hackNewLootTableContext(new ResourceLocation(category,entry),false);
		contextQ.get().push(ctx);
	}

	private static ThreadLocal<Deque> hackLootTableContextDeque() throws IllegalAccessException, NoSuchFieldException {
		Field variable = ForgeHooks.class.getDeclaredField(fieldName_lootContext);
		variable.setAccessible(true);
		return (ThreadLocal<Deque>) variable.get(null);
	}

	private static Object hackNewLootTableContext(ResourceLocation rsrc, boolean isCustom) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

		Class ctxClass = Class.forName(className_LootTableContext);
		Constructor constructor = ctxClass.getDeclaredConstructor(ResourceLocation.class, boolean.class);
		constructor.setAccessible(true);
		Object o = constructor.newInstance(rsrc,isCustom);
		return o;
	}


	public static LootPool[] getPoolsFromLootTable(LootTable t) throws IllegalAccessException, NoSuchFieldException {
		// time for shadow magic
		Field[] vars = LootTable.class.getDeclaredFields();
		for(Field v : vars){
			if(v.getType().isAssignableFrom(List.class)){
				v.setAccessible(true);
				if( ((List)v.get(t)).isEmpty()
						|| ((List)v.get(t)).get(0) instanceof LootPool) {
					return ((List<LootPool>)v.get(t)).toArray(new LootPool[0]);
				}
			}
		}
		throw new NoSuchFieldException("Could not find List<LootPool> field in LootTable");
	}

	@Deprecated
	public static void addPoolsToLootTable(LootTable t, List<LootPool> pools) {
		for(LootPool pool : pools) t.addPool(pool);
	}

}
