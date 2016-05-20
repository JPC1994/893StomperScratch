package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Timer;

import java.util.ArrayList;
import java.util.Iterator;
//user data is apparently henceforth banned(Kevin)
/**
 * Created by k9sty on 2016-03-12.
 */

public class ScrGame implements Screen, InputProcessor {
	GameStomper game;
	World world;
	Map map;
	OrthographicCamera camera;
	Box2DDebugRenderer debugRenderer;
	TiledMapRenderer tiledMapRenderer;
	Player player;
	SpriteBatch spriteBatch;
	EnemySpawner[] arSpawner;
	ArrayList<Bullet> bullets;
	BitmapFont font= new BitmapFont();
    boolean bReset=false;
    float elapsedtime = 0;

	ScrGame(GameStomper gameMain) {
		this.game = gameMain;

		spriteBatch = new SpriteBatch();
		bullets = new ArrayList<Bullet>();

		initializeWorld();
		initializeCamera();
		initializePlayer();
		initializeEnemySpawner();
	}

	private void initializeWorld() {
		world = new World(new Vector2(0f, -200f), true);
		// create contact listener in the class itself so i don't need to turn every variable into a static when i call it
		world.setContactListener(new ContactListener() {
			@Override
			public void beginContact(Contact contact) {
				// Unlike presolve, beginContact is called for sensors. If you want to move the
				// other hit detection code to presolve, go ahead, just leave the sensor code
				Fixture fixtureA = contact.getFixtureA();
				Fixture fixtureB = contact.getFixtureB();

				Fixture enemyFixture = (fixtureA.getFilterData().groupIndex == -2) ? fixtureA : (fixtureB.getFilterData().groupIndex == -2) ? fixtureB : null;
				Fixture bulletFixture = (fixtureA.getFilterData().groupIndex == -1) ? fixtureA : (fixtureB.getFilterData().groupIndex == -1) ? fixtureB : null;
				Fixture playerFoot = (fixtureA.getFilterData().categoryBits==3) ? fixtureA : (fixtureB.getFilterData().categoryBits==3) ? fixtureB : null;

				if (fixtureA == player.footSensor)
					player.isGrounded = true;

				else if (fixtureB == player.footSensor)
					player.isGrounded = true;

				for (int i=0; i<arSpawner.length;i++) {
					if (enemyFixture != null && (bulletFixture != null || playerFoot != null)) { // An enemy and a bullet collided
						// Find the enemy that owns this fixture
						if (playerFoot != null) {
							player.body.applyLinearImpulse(new Vector2(0, 50), new Vector2(player.getPosition()), false);
						}
						for (FastEnemy fastEnemy : arSpawner[i].fastEnemies) {
							if (fastEnemy.body.equals(enemyFixture.getBody())) {
								fastEnemy.isAlive = false;
								break;
							}
						}
					}
				}
			}

			@Override
			public void endContact(Contact contact) {
				Fixture fixtureA = contact.getFixtureA();
				Fixture fixtureB = contact.getFixtureB();

				if (fixtureA == player.footSensor)
					player.isGrounded = false;

				else if (fixtureB == player.footSensor)
					player.isGrounded = false;
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				Fixture fa = contact.getFixtureA();
				Fixture fb = contact.getFixtureB();

				Fixture enemyFixture = (fa.getFilterData().groupIndex == -2) ? fa : (fb.getFilterData().groupIndex == -2) ? fb : null;
				Fixture bulletFixture = (fa.getFilterData().groupIndex == -1) ? fa : (fb.getFilterData().groupIndex == -1) ? fb : null;
				Fixture playerBody = (fa.getFilterData().categoryBits==2) ? fa : (fb.getFilterData().categoryBits==2) ? fb : null;
				Fixture HarmfulObj = (fa.getFilterData().categoryBits==5) ? fa : (fb.getFilterData().categoryBits==5) ? fb : null;

				for (int i=0; i<arSpawner.length;i++) {
                    if(player.bImmune==false) {
                        //cycle through enemies spawned by all spawners
                        if (bulletFixture != null) { // A bullet hit something
                            // Find the bullet that owns the fixture
                            for (Bullet bullet : bullets) {
                                if (bullet.body.equals(bulletFixture.getBody())) {
                                    bullet.hasContacted = true;
                                    break;
                                }
                            }
                        }
                        if ((HarmfulObj != null || enemyFixture != null) && playerBody != null) {
                            //set all enemies and bullets to be destroyed on the next call of the clean function
                            System.out.println("Health: " + player.health);
                            System.out.println("Body contacted");
                            immunity();
                            for (FastEnemy fastEnemy : arSpawner[i].fastEnemies) {
                                fastEnemy.isAlive = false;
                            }
                            for (Bullet bullet : bullets) {
                                bullet.hasContacted = true;
                            }
                            //subtract one player life whenever in contact with a dangerous object
                            bReset = true;
                            //if the player's health reaches 0, send the user to the game over screen
                            if (player.health == 0) {
                                //sets the screen for the dead player
                                game.currentState = GameStomper.GameState.DEAD;
                                game.updateScreen();
                                reset();
                            }
                        }
                    }
				}
			}

			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {

			}
		});

		// pass world and desired map
		map = new Map(world, "debugroom");
	}

	private void initializeCamera() {
		debugRenderer = new Box2DDebugRenderer();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 32 * (19 / 2), 32 * (10 / 2));
		// tile size * first two digits of resolution give you a solid camera, i just divide by 2 for a better view
		// two is a magic number
		camera.update();
		tiledMapRenderer = new OrthogonalTiledMapRenderer(map.getMap(), map.getUnitScale());
		// important: go to getUnitScale function in Map
	}

	private void initializePlayer() {
		player = new Player(world, map.getPlayerSpawnPoint());
        immunity();
	}

	private void initializeEnemySpawner() {
		Vector2[] arEnemySpawnPoints = map.getEnemySpawnPoints();
		arSpawner = new EnemySpawner[map.nSpawners];
		for (int i = 0; i < arSpawner.length; i++) {
			arSpawner[i] = new EnemySpawner(world, arEnemySpawnPoints[i]);
		}

	}


	@Override
	public void show() {
		Gdx.input.setInputProcessor(this);
	}

	@Override
	public void render(float delta) {
		// Update all our stuff before rendering
        elapsedtime++;
		player.bulletCooldown--;
		player.move();

		for (EnemySpawner enemySpawner: arSpawner)
			enemySpawner.update(player.getPosition());

		world.step(1 / 60f, 6, 2); // Update our world

		clean(); // Remove dead enemies and collided bullets

		camera.position.set(new Vector3(player.getPosition().x, player.getPosition().y, 0f)); // Center the screen on the player
		camera.update(); // Lol idk

		// Rendering things...
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen

		tiledMapRenderer.setView(camera);
		tiledMapRenderer.render(); // Render the map
		debugRenderer.render(world, camera.combined); // Render the outlines on objects

		spriteBatch.setProjectionMatrix(camera.combined);
		// set the projection matrix as the camera so the tile layer on the map lines up with the bodies
		// if this line wasn't here it wouldn't scale down
		spriteBatch.begin();
		player.draw(spriteBatch);
		for(EnemySpawner enemySpawner : arSpawner)
			enemySpawner.draw(spriteBatch);
        font.draw(spriteBatch, "Health: " + (player.health+1), player.getPosition().x - 130f, player.getPosition().y + 75f);
        font.draw(spriteBatch, "Time: " + (int) (elapsedtime/60), player.getPosition().x - 15f, player.getPosition().y + 75f);
        font.draw(spriteBatch, "Level 1", player.getPosition().x + 90f, player.getPosition().y + 75f);
		spriteBatch.end();
	}

	private void immunity(){
		player.bImmune=true;
        System.out.println("immune");
        //timer code from intothewoods group:https://github.com/spidermanchild/IntoTheWoodsMultScreens
		Timer.schedule(new Timer.Task() {
			@Override
			public void run() {
				player.bImmune=false;
                System.out.println("vulnerable");
			}
		}, 5);
	}
	//TODO: respawn a new player from player spawn.
    private void reset(){
        //to include other property resets later
        player.health=player.MAX_HEALTH;
        elapsedtime=0;
    }

	private void clean() {
		// We have to remove stuff here instead of in the contact listener because it will crash
		// because (my guess) of a ConcurrentModificationException.

		Iterator<Bullet> bulletIterator = bullets.iterator();
		while (bulletIterator.hasNext()) {
			Bullet b = bulletIterator.next();
			if (b.hasContacted) {
				world.destroyBody(b.body);
				bulletIterator.remove();
			}
		}

		for (EnemySpawner enemySpawner : arSpawner) {
			Iterator<FastEnemy> enemyIterator = enemySpawner.fastEnemies.iterator();
			while (enemyIterator.hasNext()) {
				FastEnemy enemy = enemyIterator.next();
				if (!enemy.isAlive) {
					world.destroyBody(enemy.body);
					enemyIterator.remove();
				}
			}
		}
        //if the player contacts an enemy or obstacle, subtract one life from the player
        if (bReset){
            player.health-=1;
            player.immunity=10f;
            bReset=false;
        }
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean keyDown(int keycode) {
		// TODO: Should this be moved to player class as well?
		if (keycode == Input.Keys.X && player.bulletCooldown <= 0) {
			bullets.add(new Bullet(world, player.getPosition(), player.bRight));
			player.bulletCooldown = 30;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}

//CategoryBit List
// 1 Ground
// 2 Player
// 3 Player Foot
// 4 Bullet
// 5 Harmful Obstacles (Spikes, etc.)
// 6
// 7
// 8 Fast Enemy