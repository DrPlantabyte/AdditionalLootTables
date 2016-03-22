
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
package cyano.alt.examples;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Created by Chris on 3/21/2016.
 */
public class Examples {
	private static final String domainName = "example";

	private static final String CHEST_EXAMPLE =
			"{\n" +
			"    \"__comment_line1\":\"This example adds a 25% of finding a block or iron in a village blacksmith \",\n" +
			"    \"__comment_line2\":\"chest and a 100% chance of adding 1 or 2 hoes. Note that these pools \",\n" +
			"    \"__comment_line3\":\"are added in addition to whatever is specified in the world's loot_tables \",\n" +
			"    \"__comment_line4\":\"folder (default loot tables are not replaced)\",\n" +
			"    \"pools\": [\n" +
			"        {\n" +
			"            \"rolls\": 1,\n" +
			"            \"entries\": [\n" +
			"                {\n" +
			"                    \"type\": \"empty\",\n" +
			"                    \"weight\": 75\n" +
			"                },\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:iron_block\",\n" +
			"                    \"functions\": [\n" +
			"                        {\n" +
			"                            \"function\": \"set_count\",\n" +
			"                            \"count\": 1\n" +
			"                        }\n" +
			"                    ],\n" +
			"                    \"weight\": 25\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"           \"rolls\": {\n" +
			"                \"min\": 1,\n" +
			"                \"max\": 2\n" +
			"            },\n" +
			"            \"entries\": [\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:wooden_hoe\",\n" +
			"                    \"weight\": 8\n" +
			"                },\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:stone_hoe\",\n" +
			"                    \"weight\": 4\n" +
			"                },\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:iron_hoe\",\n" +
			"                    \"weight\": 2\n" +
			"                },\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:golden_hoe\",\n" +
			"                    \"weight\": 1\n" +
			"                }\n" +
			"            ]\n" +
			"        }\n" +
			"    ]\n" +
			"}";
	private static final String SKELETON_EXAMPLE =
			"{\n" +
			"    \"__comment_line1\":\"This example makes skeletons always drop a bow. Note that this pool \",\n" +
			"    \"__comment_line3\":\"is added in addition to whatever is specified in the world's loot_tables \",\n" +
			"    \"__comment_line4\":\"folder (default loot tables are not replaced)\",\n" +
			"    \"pools\": [\n" +
			"        {\n" +
			"            \"rolls\": 1,\n" +
			"            \"entries\": [\n" +
			"                {\n" +
			"                    \"type\": \"item\",\n" +
			"                    \"name\": \"minecraft:bow\",\n" +
			"                    \"functions\": [\n" +
			"                        {\n" +
			"                            \"function\": \"set_count\",\n" +
			"                            \"count\": 1\n" +
			"                        },\n" +
			"                        {\n" +
			"                            \"function\": \"set_damage\",\n" +
			"                            \"damage\": {\n" +
			"                                \"min\":0.1,\n" +
			"                                \"max\":0.3\n" +
			"                            }\n" +
			"                        }\n" +
			"                    ],\n" +
			"                    \"weight\": 1\n" +
			"                }\n" +
			"            ],\n" +
			"            \"conditions\": [\n" +
			"                {\n" +
			"                    \"condition\": \"killed_by_player\"\n" +
			"                }\n" +
			"            ]\n" +
			"        }\n" +
			"    ]\n" +
			"}";

	public static void makeChestExample(Path lootFolder) throws IOException {
		Path dir = lootFolder.resolve(domainName).resolve("chests");
		Files.createDirectories(dir);
		Path file = dir.resolve("village_blacksmith.json");
		Files.write(file, Arrays.asList(CHEST_EXAMPLE), Charset.forName("UTF-8"));
	}
	public static void makeEntityExample(Path lootFolder) throws IOException {
		Path dir = lootFolder.resolve(domainName).resolve("entities");
		Files.createDirectories(dir);
		Path file = dir.resolve("skeleton.json");
		Files.write(file, Arrays.asList(SKELETON_EXAMPLE), Charset.forName("UTF-8"));
	}
}
