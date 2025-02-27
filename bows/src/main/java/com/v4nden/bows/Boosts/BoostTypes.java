package com.v4nden.bows.Boosts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public enum BoostTypes {
	SPEED(Material.LIGHT_BLUE_DYE, 1, "speed", "Скорость",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Накладывает на владельца\n буста эффект скорости\n первого уровня на 10 секунд",
			player -> {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 1));

				player.getLocation().getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 100,
						2, 2,
						2);

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
							SoundCategory.MASTER,
							1,
							1);
				}

			}, 3),

	RAIN(Material.ORANGE_DYE, 3, "rain", "Дождь из стрел", ChatColor.of("#a6a6a6") + "При использовании:"
			+ ChatColor.RESET
			+ "\n Бросает заряд в сторону\n направления головы игрока, \n на точке где заряд \n контактировал с блоком\n вызывает дождь из стрел\n в радиусе 5ти блоков",
			player -> {

				Snowball charge = BowsUtils.lauchCharge(player, Material.ORANGE_DYE);
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
							SoundCategory.MASTER, 1,
							1);
				}
				new TemporaryListener<>(ProjectileHitEvent.class,
						EventPriority.NORMAL, e -> {
							ProjectileHitEvent event = (ProjectileHitEvent) e;
							if (event.getEntity().equals(charge)) {
								Location loc = event.getEntity().getLocation();

								for (Player p : Bukkit.getOnlinePlayers()) {
									p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
											SoundCategory.MASTER, 5,
											1);
									p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
											SoundCategory.MASTER, 5,
											1);
									p.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_6,
											SoundCategory.MASTER, 5,
											1);
								}

								for (int i = 0; i <= 100; i += 2) {
									Random random = new Random();
									Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
										loc.getWorld().spawnArrow(
												loc.clone()
														.add(new Vector(
																5 - random.nextInt(
																		10)
																		+ random.nextFloat(),
																50,
																5 - random.nextInt(
																		10)
																		+ random.nextFloat())),
												new Vector(0, -3, 0), 2f, .1f);

									}, i);
								}
								return true;
							}
							return false;

						});

			}),
	BRIDGE(Material.LIME_DYE, 2, "bridge", "Мостик", ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
			+ "\n Бросает заряд в сторону\n направления головы игрока, \n по траектории пути заряда\n в воздухе оставляет платформы\n 3x3 из блоков, которые\n пропадают спустя 10 секунд",
			player -> {
				Snowball charge = BowsUtils.lauchCharge(player, Material.LIME_DYE);

				int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
					Location loc = charge.getLocation().clone().add(0, -3, 0);
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							loc.clone().add(x, 0, z).getBlock().setType(Material.LIME_CONCRETE);
						}
					}

					for (Player p : Bukkit.getOnlinePlayers()) {
						p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER,
								3,
								1);
					}
					Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
						for (int x = -1; x <= 1; x++) {
							for (int z = -1; z <= 1; z++) {
								loc.clone().add(x, 0, z).getBlock().setType(Material.AIR);
							}
						}
						for (Player p : Bukkit.getOnlinePlayers()) {
							p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER,
									3,
									1);
						}
					}, 200L);
				}, 0, 1L);

				new TemporaryListener<>(ProjectileHitEvent.class,
						EventPriority.NORMAL, e -> {
							ProjectileHitEvent event = (ProjectileHitEvent) e;
							if (event.getEntity().equals(charge)) {
								Bukkit.getScheduler().cancelTask(task);
								return true;
							}
							return false;
						});

			}),

	DRILL(Material.NETHERITE_SCRAP, 4, "drill", "Бур", ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
			+ "\n НЕТ ОПИСАНИЯ",
			player -> {
				Snowball charge = BowsUtils.lauchCharge(player, Material.NETHERITE_SCRAP);
				double created = System.currentTimeMillis();

				int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
					double percent = 1 - (System.currentTimeMillis() - created) / 3000;
					for (int x = -3; x <= 3; x++) {
						for (int z = -3; z <= 3; z++) {
							for (int y = -3; y <= 3; y++) {
								Block block = player.getWorld()
										.getBlockAt(charge.getLocation().clone().add(Math.round(percent * x),
												Math.round(percent * y), Math.round(percent * z)));

								if (block.getType().equals(Material.AIR))
									continue;
								charge.getWorld().playSound(charge.getLocation(),
										block.getBlockData().getSoundGroup().getBreakSound(), 3, 1);
								double random = Math.random();
								if (random > .9) {
									player.getInventory().addItem(block.getDrops().toArray(new ItemStack[0]));
								} else if (random > .5) {
									Item drop = block.getWorld().spawn(block.getLocation(), Item.class);
									ItemStack[] drops = block.getDrops().toArray(new ItemStack[0]);
									if (drops.length > 0)
										drop.setItemStack(drops[0]);
								}

								block.setType(Material.AIR);
							}
						}
					}
				}, 0, 1L);

				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					Bukkit.getServer().getScheduler().cancelTask(task);
				}, 60);

			}),
	ARROWGUN(Material.BLAZE_POWDER, 4, "arrowgun", "Стреломёт",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n НЕТ ОПИСАНИЯ",
			player -> {
				List<Arrow> arrows = new ArrayList<Arrow>();
				double now = System.currentTimeMillis();

				int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2, 1);
					Location location = player.getLocation().add(0, 1.5, 0);
					for (int i = 0; i < 2; i++) {
						arrows.add(
								location.getWorld().spawnArrow(location, player.getLocation().getDirection(),
										Double.valueOf(3 * Math.random() + .5).floatValue(), 10));
					}

				}, 0, 2L);

				Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
					Bukkit.getScheduler().cancelTask(task);
				}, 60L);

				new TemporaryListener<>(ProjectileHitEvent.class, EventPriority.NORMAL, e -> {
					ProjectileHitEvent event = (ProjectileHitEvent) e;
					if (System.currentTimeMillis() - now < 15000) {
						if (event.getHitBlock() != null && arrows.contains(event.getEntity())) {
							event.getEntity().remove();
						}

						return false;
					} else {
						arrows.clear();
						System.out.println("Unregistered projectile event");
						return true;
					}

				});

			}, 3),
	RIDER(Material.BOWL, 2, "rider", "Райдер", ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
			+ "\n Бросает заряд в сторону\n направления головы игрока, \n игрок становится пассажиром\n заряда и двигается по\n траектории его полёта\n в воздухе",
			player -> {
				Snowball charge = BowsUtils.lauchCharge(player, Material.BOWL);
				charge.addPassenger(player);

			}, .5f),
	TELEPORTRANDOM(Material.ENDER_PEARL, 2, "teleportrandom", "Телепорт к рандомному игроку",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Спустя задержку телепортирует\n владельца к случайному\n игроку в текущей игре, звук\n задержки перед телепортацией\n слышан как владельцу, так и\n случайному игроку",
			player -> {

				Game.gamePlayers.remove(player);
				int rnd = new Random().nextInt(Game.gamePlayers.size());
				Player randomplayer = Game.gamePlayers.get(rnd);
				Game.gamePlayers.add(player);

				player.sendTitle("3...", "Телепортация через", 5, 10, 5);
				player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);
				randomplayer.playSound(randomplayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);

				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.sendTitle("2...", "Телепортация через", 5, 10, 5);
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
					randomplayer.playSound(randomplayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
				}, 20);
				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.sendTitle("1...", "Телепортация через", 5, 10, 5);
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
					randomplayer.playSound(randomplayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
				}, 40);
				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.teleport(randomplayer.getLocation());
					player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
				}, 60);

			}),
	TELEPORTCLOSEST(Material.ENDER_PEARL, 3, "teleportclosest", "Телепорт к ближайшему игроку",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Спустя задержку телепортирует\n владельца к ближайшему\n игроку в текущей игре, звук\n задержки перед телепортацией\n слышан как владельцу, так и\n ближайшему игроку",
			player -> {

				Game.gamePlayers.remove(player);
				Player closest = Game.gamePlayers.stream()
						.min(Comparator.comparingDouble(
								(gamePlayer) -> gamePlayer.getLocation().distance(player.getLocation())))
						.get();
				Game.gamePlayers.add(player);

				player.sendTitle("3...", "Телепортация через", 5, 10, 5);
				player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);
				closest.playSound(closest.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);

				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.sendTitle("2...", "Телепортация через", 5, 10, 5);
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
					closest.playSound(closest.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
				}, 20);
				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.sendTitle("1...", "Телепортация через", 5, 10, 5);
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
					closest.playSound(closest.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
				}, 40);
				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.teleport(closest.getLocation());
					player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
				}, 60);

			}),

	GLOWCLOSEST(Material.GLOWSTONE_DUST, 2, "glowclosest", "Подсветить ближайшего игрока",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Спустя задержку перед применением\n выдаёт ближайшему к владельцу\n игроку эффект свечения, звук\n задержки перед эффектом\n слышан как владельцу, так и\n ближайшему игроку",
			player -> {

				Game.gamePlayers.remove(player);
				Player closest = Game.gamePlayers.stream()
						.min(Comparator.comparingDouble(
								(gamePlayer) -> gamePlayer.getLocation().distance(player.getLocation())))
						.get();
				Game.gamePlayers.add(player);

				closest.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 5, 1));

			}, 1),

	LOCATOR(Material.ECHO_SHARD, 1, "locator", "Локатор",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n НЕТ ОПИСАНИЯ",
			player -> {

				Game.gamePlayers.remove(player);
				Player closest = Game.gamePlayers.stream()
						.min(Comparator.comparingDouble(
								(gamePlayer) -> gamePlayer.getLocation().distance(player.getLocation())))
						.get();
				Game.gamePlayers.add(player);

				for (int i = 0; i <= 20; i++) {
					final Integer fi = Integer.valueOf(i);
					Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
						player.getWorld().playSound(closest.getLocation(), Sound.ENTITY_ALLAY_ITEM_THROWN,
								5,
								Double.valueOf((2.1 - (((fi + 1) % 5) / 2.0))).floatValue());
					}, i * 10);
				}

			}),

	GLOWRANDOM(Material.GLOWSTONE_DUST, 2, "glowrandom", "Подсветить рандомного игрока",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Спустя задержку перед применением\n выдаёт случайному игрокув текущей\n игре эффект свечения, звук задержки\n перед эффектом слышан\n как владельцу, так и\n случайному игроку",
			player -> {

				Game.gamePlayers.remove(player);
				int rnd = new Random().nextInt(Game.gamePlayers.size());
				Player randomplayer = Game.gamePlayers.get(rnd);
				Game.gamePlayers.add(player);

				randomplayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 5, 1));

			}, 1),

	GLOWALL(Material.GLOWSTONE_DUST, 5, "glowall", "Подсветить всех игроков",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Спустя задержку перед применением\n выдаёт всем игрокам эффект\n свечения, звук задержки перед\n эффектом слышан как владельцу,\n так и всем игрокам",
			player -> {

				Game.gamePlayers.forEach((gamePlayer) -> {

					gamePlayer.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0);
					gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 15, 1));
					gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));

				});

			}, 1.5f),

	TIMESTOP(Material.NETHER_STAR, 5, "timestop", "Таймстоп", ChatColor.of("#a6a6a6") + "При использовании:"
			+ ChatColor.RESET
			+ "\n Останавливает время для всех игроков\n на 10 секунд. В остановленном времени\n стрелы, заряды и остальные факторы\n игровой физики будут заморожены.\n Все игроки кроме вас не смогут\n двигаться. При использовании \n таймстопа в уже остановленном\n времени буст безвозвратно пропадёт",
			player -> {
				if (Bukkit.getServerTickManager().isFrozen()) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.playSound(p.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH,
								SoundCategory.MASTER,
								100,
								1);
					}
					return;
				}

				long now = System.currentTimeMillis();
				Bukkit.getServerTickManager().setFrozen(true);

				Game.gamePlayers.forEach((gamePlayer) -> {
					gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
					gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 10, 1));
					if (!gamePlayer.equals(player)) {
						gamePlayer.setAllowFlight(true);
					}
				});

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,
							SoundCategory.MASTER,
							100,
							1);
				}

				for (int i = 0; i <= 200; i += 40) {
					Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
						for (Player p : Bukkit.getOnlinePlayers()) {
							p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON,
									SoundCategory.MASTER,
									10,
									1);
						}
					}, i);
				}
				for (int i = 20; i <= 200; i += 40) {
					Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
						for (Player p : Bukkit.getOnlinePlayers()) {
							p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF,
									SoundCategory.MASTER,
									10,
									1);
						}
					}, i);
				}

				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					Game.gamePlayers.forEach((gamePlayer) -> {
						gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
						if (!gamePlayer.equals(player)) {
							gamePlayer.setAllowFlight(false);
						}
					});
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
								SoundCategory.MASTER,
								100,
								1);
					}
					Bukkit.getServerTickManager().setFrozen(false);
				}, 200L);

				new TemporaryListener<>(PlayerMoveEvent.class, EventPriority.NORMAL, e -> {
					PlayerMoveEvent event = (PlayerMoveEvent) e;
					if (System.currentTimeMillis() - now < 10000) {
						if (event.getPlayer().equals(player) || !Game.gamePlayers.contains(event.getPlayer())) {
							event.setCancelled(false);
						} else {
							event.setCancelled(true);
						}

						return false;
					} else {
						System.out.println("Unregistered move event");
						return true;
					}

				});

			}, 5),
	BREAK(Material.FIREWORK_STAR, 4, "break", "Удар", ChatColor.of("#a6a6a6")
			+ "При использовании:" + ChatColor.RESET
			+ "\n Подбрасывает владельца\n в воздух, затем с силой\n ударяет о поверхность,\n что создаёт ударную\n волну отталкивающую и \n наносящую урон игрокам\n в радиусе 10 блоков",
			player -> {
				player.setVelocity(player.getVelocity().add(new Vector(0, 1.15, 0)));
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
							SoundCategory.MASTER, 1,
							1);
				}
				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {

					player.setVelocity(player.getVelocity().add(new Vector(0, -2, 0)));
				}, 15L);

				Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
					player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
							player.getLocation(),
							50, 3,
							0, 3, .01);
					Game.gamePlayers.forEach((gamePlayer) -> {
						if (!gamePlayer.equals(player)) {
							if (gamePlayer.getLocation().distance(player.getLocation()) < 10) {
								gamePlayer.setVelocity(gamePlayer.getVelocity().add(new Vector(0, 1.5, 0)));
							}
						}
					});
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK,
								SoundCategory.MASTER,
								1,
								1);
					}

				}, 19L);

			}),
	TNT(Material.TNT, 2, "tnt", "Динамит", ChatColor.of("#a6a6a6")
			+ "При использовании:" + ChatColor.RESET
			+ "\n Запускает заряженый динамит \n в сторону направления\n головы игрока",
			player -> {
				TNTPrimed tnt = player.getWorld().spawn(player.getLocation().add(new Vector(0, 1, 0)),
						TNTPrimed.class);
				tnt.setVelocity(player.getLocation().getDirection().multiply((2)));

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, SoundCategory.MASTER,
							1,
							1);
					p.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW,
							SoundCategory.MASTER,
							1,
							1);
				}

			}, .7f),
	METEOR(Material.FIRE_CHARGE, 2, "meteor", "Метеор", ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
			+ "\n Бросает заряд в сторону\n направления головы игрока, \n на точке где заряд \n контактировал с блоком\n вызывает заряженный\n динамит падающий сверху",
			player -> {
				Snowball charge = BowsUtils.lauchCharge(player, Material.FIRE_CHARGE);
				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
							SoundCategory.MASTER, 1,
							1);
				}
				new TemporaryListener<>(ProjectileHitEvent.class,
						EventPriority.NORMAL, e -> {
							ProjectileHitEvent event = (ProjectileHitEvent) e;
							if (event.getEntity().equals(charge)) {
								Location loc = event.getEntity().getLocation();

								player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 100, 1);

								for (int i = 0; i <= 30; i++) {
									final Integer fi = Integer.valueOf(i);
									Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
										player.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 15, fi / 9);
									}, i * 2);
								}

								Game.gamePlayers.forEach((gamePlayer) -> {
									if (gamePlayer.getLocation().distance(loc) < 15) {
										gamePlayer.sendTitle("МЕТЕОРИТ", "!!!", 3, 15, 3);
									}
								});

								TNTPrimed tnt = player.getWorld().spawn(loc.add(new Vector(0, 150, 0)),
										TNTPrimed.class);
								tnt.setVelocity(new Vector(0, -3, 0));
								tnt.setIsIncendiary(true);
								tnt.setFuseTicks(60);

								return true;
							}
							return false;

						});

			}),
	GLASSPILAR(Material.GLASS, 1, "glasspilar", "Столб", ChatColor.of("#a6a6a6")
			+ "При использовании:" + ChatColor.RESET
			+ "\n Создаёт под владельцем\n столб из стекла 3х3х4\n и перемещает игрока на\n его верх",
			player -> {
				for (int y = 0; y < 4; y++) {
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							player.getLocation().add(new Vector(x, y, z)).getBlock()
									.setType(Material.GLASS);
						}
					}

				}
				player.teleport(player.getLocation().add(0, 4, 0));

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, SoundCategory.MASTER,
							1,
							1);
				}

			}, 0.5f),
	GLASSBOX(Material.GLASS, 1, "glassbox", "Коробка",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Создаёт вокруг владельца\n коробку из стекла 5х5х3",
			player -> {
				for (int y = 0; y < 3; y++) {
					player.getLocation().add(new Vector(-1, y, 2)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(0, y, 2)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(1, y, 2)).getBlock()
							.setType(Material.GLASS);

					player.getLocation().add(new Vector(-1, y, -2)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(0, y, -2)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(1, y, -2)).getBlock()
							.setType(Material.GLASS);

					player.getLocation().add(new Vector(2, y, 1)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(2, y, 0)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(2, y, -1)).getBlock()
							.setType(Material.GLASS);

					player.getLocation().add(new Vector(-2, y, 1)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(-2, y, 0)).getBlock()
							.setType(Material.GLASS);
					player.getLocation().add(new Vector(-2, y, -1)).getBlock()
							.setType(Material.GLASS);

				}

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, SoundCategory.MASTER,
							1,
							1);
				}

			}, 0.5f),
	BLINDNESSAURA(Material.BLACK_DYE, 1, "blindnessaura", "Аура слепоты",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Создаёт вокруг владельца\n коробку из стекла 5х5х3",
			player -> {
				Random random = new Random();

				int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {

					Game.gamePlayers.forEach((gamePlayer) -> {
						if (gamePlayer != player) {
							if (gamePlayer.getLocation().distance(player.getLocation()) < 50) {
								gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 1));
							}
						}
					});

					Location location = player.getLocation();
					for (double i = 0; i <= Math.PI; i += Math.PI / 20) {
						double radius = Math.sin(i) * 20;
						double y = Math.cos(i) * 20;
						for (double a = 0; a < Math.PI * 2; a += Math.PI / 20) {
							double x = Math.cos(a) * radius;
							double z = Math.sin(a) * radius;

							player.getLocation().getWorld().spawnParticle(Particle.DUST,
									location.clone().add(x + random.nextFloat(),
											y + random.nextFloat(),
											z + random.nextFloat()),
									1,
									new Particle.DustOptions(
											Color.fromRGB(46, 46, 46), 5));

						}
					}
				}, 0, 3L);

				Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
					Bukkit.getScheduler().cancelTask(task);
				}, 120L);

			}, 3),
	DASH(Material.FEATHER, 1, "dash", "Рывок",
			ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
					+ "\n Запускает владельца\n относительно поворота головы\n вперёд ~30 блоков",
			player -> {
				player.setVelocity(player.getLocation().getDirection().multiply((2)));

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP,
							SoundCategory.MASTER, 1,
							1);
				}

			});

	public Material material;
	public int rarity;
	public String id;
	public String name;
	public String description;
	public float delay;
	private Consumer<Player> runnable;

	BoostTypes(Material material, int rarity, String id, String name, String description,
			Consumer<Player> runnable) {
		this.material = material;
		this.rarity = rarity;
		this.id = id;
		this.name = name;
		description += "\n\n";
		description += ChatColor.of("#939393") + "⌚: Моментально";
		description += "\n";
		this.description = description;
		this.runnable = runnable;
	}

	BoostTypes(Material material, int rarity, String id, String name, String description,
			Consumer<Player> runnable, float delay) {
		this.material = material;
		this.rarity = rarity;
		this.id = id;
		this.name = name;
		description += "\n\n";
		description += ChatColor.of("#939393") + "⌚: " + String.valueOf(delay > 0 ? delay : delay * 1000)
				+ (delay > 0 ? "s" : "ms");
		description += "\n";
		this.description = description;
		this.runnable = runnable;
		this.delay = delay;
	}

	public Boost getBoost(Player owner) {
		return new Boost(owner, this.material, this.rarity, this.id, this.name, this.description,
				this.runnable, this.delay);
	}
}
