package jme3test.helloworld;

import ai.competitor.dodgeshooter.DodgeShooterAI;
import ai.defense.dodging.AerialDodgingAI;
import ai.defense.dodging.DodgingAI;
import ai.defense.dodging.WalkingGroundDodgingAI;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import constrainedcamera.ConstrainedSimpleApplication;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import poormocap.Mocap;

/**
 * Based on jMonkeyEngine 3 Example 9 by normen, with edits by Zathras.
 *
 * @author Peter Purwanto
 * @author Zhan Feng
 * @author Charles Langley
 * @author Tuan Tran
 */
public class FPSGame extends ConstrainedSimpleApplication
        implements ActionListener {
// TODO: Generate golems with random starting locations?
    /*--- SETTINGS ---*/
    /* Total time limit for every round.
     * Unit is in seconds (e.g.: 5*60=300 seconds / 5 minutes)
     */

    public static final long DEFAULT_ROUND_TIME_LIMIT_SECONDS = 5 * 60;
    public static final int DEFAULT_ENEMY_HIT_POINTS = 20;
    /**
     * Used to enforce fire rate limit: Allow to fire, at most, every this many
     * milliseconds. e.g.: 500 = 2 shots per second; 3000 = 1 shot per 3
     * seconds. Not a constant to allow room for if there are
     * powerups/situations that can change fire rate or slow it down. This will
     * be used when comparing the last time player shoots
     * (lastShotFiredTimeMillis) and current time of when player's about to
     * shoot again. (Unit: milliseconds.)
     */
    private static int maxFireRateMillis = 100;
    private static float maxShootRange = 100f;
    // Scoring
    public static final long MAX_TOTAL_SCORE = 1000000;
    private static int movingTargetsHitScoreBonus = 1;
    private static int stationaryTargetsHitScoreBonus = 0;
    private static int quickDestroyScoreBonus = 50;
    private static int quickestDestroyScoreBonus = 100;
    private static int stationaryTargetsDestroyScoreBonus = 10;
    private static int movingTargetsDestroyScoreBonus = 100;
    /**
     * In addition to other destroy bonuses, add this.
     */
    private static int attackingTargetsExtraDestroyScoreBonus = 100;
    private static int winGameScoreBonus = 100;
    // Styling
    private static int hudTimerFontSizeMultiplier = 2;
    private static int hudScoreFontSizeMultiplier = 2;
    private static int hudEnemyCounterFontSizeMultiplier = 2;
    private static int hudGoalFontSizeMultiplier = 10;
    private static int hudKinectControlsStatusFontSizeMultiplier = 1;
    public static final float LASER_BEAM_SHRINK_EXPAND_RATE = 2;
    /**
     * How long to keep showing hit marker once it's shown. (Unit:
     * milliseconds.)
     */
    public static final int SHOW_HIT_MARKER_MAX_DURATION_MILLIS = 250;
    // Positioning
    public static final Vector3f DEFAULT_PLAYER_LOCATION = new Vector3f(5, 5, 20);
    /**
     * Initial camera and "up" directions
     */
    private static Vector3f camInitDir = null, camInitUp = null; // Use the defaults
    /**
     * Minimum location y-axis that the player and objects can be at and still
     * let the game be playable.
     */
    public static final float MIN_PLAYABLE_LOCATION_Y_AXIS = -150;
    // Kinect
    /**
     * Toggle kinect use. If false, loading PoorMoCap/reading the Kinect won't
     * be attempted.
     */
    private static final boolean ENABLE_KINECT_INPUT = true;
    /**
     * This effectively controls kinect movement sensitivity. Example values:
     * 0.001 = 1 mm (original); 1 = 1 meter (jMonkey unit); 10 = 10 meters; ...
     * Example case: If Kinect user moves for 300 mm (or 30 cm), multiplier of 1
     * would make the game move 300 meters. And multiplier of 1,000 would make
     * the game move 300 km (or 300,000 meters)!
     */
    private static float kinectTurnLeftRightMultiplier = 10000;
    private static float kinectLookUpDownMultiplier = 10000;
    /**
     * Mapping for KinectTCP's skeleton Joint [Array] Index System
     */
    public static final int SPINE = 1,
            SHOULDER_LEFT = 4,
            WRIST_LEFT = 6,
            SHOULDER_CENTER = 2,
            SHOULDER_RIGHT = 8,
            WRIST_RIGHT = 10;
    /**
     * Mapping for any KinectTCP's Joint's X, Y, and Z coordinates' array
     * indexes. E.g., joints[WRIST_RIGHT][KJ_X] gets you right wrist Kinect
     * Joint position's x coordinate.
     */
    public static final int KJ_X = 1,
            KJ_Y = 2,
            KJ_Z = 3;
    /*--- END SETTINGS ---*/
    // TODO: Also use a variable to remember highest score ever for any round. Even save to/read from a text file for highest score ever on the local machine!
    private long remainingRoundTimeSec;
    private long totalScore;
    private boolean gameOver;
    /**
     * Used together with maxFireRateMillis setting to enforce fire rate limit.
     * This will be used to remember when was the last time the player shoots.
     * Only update this value when player shoots and this has no old value or
     * (currentTimeMillis - maxFireRateMillis) has passed the old time value.
     */
    private long lastShotFiredTimeMillis;
    /**
     * Used together with SHOW_HIT_MARKER_MAX_DURATION_MILLIS.
     */
    private long hitMarkerLastShownTimeMillis;
    private static FPSGame app;
    /**
     * Stores the tracked status of the kinect skeleton readings. If able to get
     * the skeleton readings, then true; otherwise false. Should be initialized
     * to false.
     */
    private static boolean kinectSkeletonActive = false;
    private int[][] joints;
    private int spine[], spineX, spineY, spineZ,
            leftShoulder[], leftShoulderX, leftShoulderY, leftShoulderZ,
            leftWrist[], leftWristX, leftWristY, leftWristZ,
            centerShoulder[], centerShoulderX, centerShoulderY, centerShoulderZ,
            rightShoulder[], rightShoulderX, rightShoulderY, rightShoulderZ,
            rightWrist[], rightWristX, rightWristY, rightWristZ;
    private static Mocap kinect = null;
    private Material kinectControlsStatusTextBackgroundMaterial;
    private Spatial sceneModel, weapon;
    /**
     * List of enemy objects that can be destroyed by and give points to the
     * player.
     */
    private List<Geometry> enemyGeoms = new ArrayList<Geometry>();
    /**
     * Stores remaining hit points (health) of enemies
     */
    private Map<Geometry, Integer> enemyHitPoints = new HashMap<Geometry, Integer>();
    /**
     * List of enemy objects that are also dodging AIs. Used to keep tracking of
     * the mapping of the spatials and dodging AI logic.
     */
    private Map<Geometry, DodgingAI> dodgingAIs = new HashMap<Geometry, DodgingAI>();
    private Map<Geometry, DodgeShooterAI> dodgeShooterAIs = new HashMap<Geometry, DodgeShooterAI>();
    // TODO: Maybe use this map for these: Map<Spatial,Geometry> enemies = new HashMap<Spatial,Geometry>();
    private Spatial golemSpatial, golemSpatial2, golemSpatial3, ninjaSpatial, hoverTankSpatial, rockshooterSpatial, golemSpatial4, golemSpatial5;
    private Geometry golemGeom, golemGeom2, golemGeom3, ninjaGeom, hoverTankGeom, rockshooterGeom, golemGeom4, golemGeom5;
    private BulletAppState bulletAppState;
    private PhysicsSpace physicsSpace;
    private RigidBodyControl landscape;
    private CharacterControl player;
    private BitmapText crosshair, hudTimer, hudScore, hudEnemyCounter;
    private Vector3f walkDirection = new Vector3f();
    /**
     * Stores interpretations of input reading values. E.g., if inputIsShooting
     * is true, that means the user is feeding input (e.g., by mouse, keyboard,
     * etc.) that is to be interpreted as the user wanting to do a shooting
     * action. While inputIsMoveLeft, inputIsMoveRight, inputIsMoveUp,
     * inputIsMoveDown are movement actions.
     */
    private boolean inputIsMoveLeft = false, inputIsMoveRight = false, inputIsMoveUp = false, inputIsMoveDown = false, inputIsShooting = false;
    private RigidBodyControl weapon_phy;
    private float time = 0;
    private int state = 0;
    private Node explosionEffect = new Node("explosionFX");
    private ParticleEmitter flame, flash, spark, roundspark, smoketrail, debris,
            shockwave;
    private static final int COUNT_FACTOR = 1;
    private static final float COUNT_FACTOR_F = 1f;
    private static final boolean POINT_SPRITE = true;
    private static final ParticleMesh.Type EMITTER_TYPE = POINT_SPRITE ? ParticleMesh.Type.Point : ParticleMesh.Type.Triangle;
    private Spatial house, stairs;
    private Cylinder laserCylinder;
    private Geometry laserBeam;
    private Vector3f laserBeamOrigScale;
    private FilterPostProcessor fpp;
    private BloomFilter bloom;
    private Material enemyBombMat;
    private static final Sphere ballSphere;
    private RigidBodyControl ball_phy;
    private AudioNode audio_gun;
    private AudioNode audio_explode;
    private AudioNode audio_background;
    private AudioNode audio_win;
    private AudioNode audio_lose;
    private AudioNode audio_impact;
    private Spatial hoverTankSpatial2;
    private Geometry hoverTankGeom2;
    private Spatial hoverTankSpatial3;
    private Geometry hoverTankGeom3;

    static { // This is the static/global "Constructor", usually used to initialize static variables.
        /**
         * Initialize the cannon ball geometry
         */
        ballSphere = new Sphere(32, 32, 1f, true, false);
        ballSphere.setTextureMode(Sphere.TextureMode.Projected);

    }

    private void resetSettings() {
        // Manually invoke the Garbage Collector after every game restarts
        System.gc();

        remainingRoundTimeSec = DEFAULT_ROUND_TIME_LIMIT_SECONDS;

        resetFireRateDelay();

        // Keep this at the very end of this method
        gameOver = false;
    }

    public static void main(String[] args) {

        Runtime JVMRuntime = Runtime.getRuntime();

        long curAvailMemorySize = JVMRuntime.totalMemory();
        System.out.println("Memory/Heap Size Currently Available = " + curAvailMemorySize + " bytes");

        long maxAvailMemorySize = JVMRuntime.maxMemory();
        System.out.println("Maximum Available Memory/Heap Size = " + maxAvailMemorySize + " bytes");

        if (ENABLE_KINECT_INPUT) {
            kinect = new Mocap();
        } else {
            kinect = null;
        }

        app = new FPSGame();

        //app.setPauseOnLostFocus(false);
        AppSettings settings = new AppSettings(true);
        // Set resolution/window size to be 75% of the monitor's resolution
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        screen.width *= 0.75;
        screen.height *= 0.75;
        settings.setResolution(screen.width, screen.height);
        settings.setFrameRate(60);
        // settings.setFullscreen(true);
        app.setSettings(settings);
        app.setShowSettings(false); // Don't show startup settings box
        // flyCam.setMoveSpeed(25); // Move this to inside simpleInitApp()

        app.start();
    }
    private Geometry hitMarker;
    private Node shootables;
    private CollisionResults collisionResultsOfShootablesWithinPlayerAim = null;
    private CameraNode camNode;
    private RigidBodyControl golem_phy;
    private RigidBodyControl ninja_phy;
    private RigidBodyControl car_phy;

    public void simpleInitApp() {

        // TODO: Put all init/reset stuff in one method for better manageability
        resetSettings();
        resetTotalScore();

        initAudio();
        initHUD();
        initSkyBox();

        //create the camera Node
        camNode = new CameraNode("Camera Node", cam);
        //This mode means that camera copies the movements of the target:
//        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        // Disable the default flyby cam
//        flyCam.setEnabled(false);
        //Attach the camNode to the target:
//        rootNode.attachChild(camNode);

        /**
         * Set up Physics
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        physicsSpace = bulletAppState.getPhysicsSpace();
        //physicsSpace.enableDebug(assetManager);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpKeys();
        setUpLight();

//        // We load the scene from the zip file and adjust its size.
//        assetManager.registerLocator("town.zip", ZipLocator.class);
//
////         Default Example Scene:
//        sceneModel = assetManager.loadModel("main.scene");
//        sceneModel.setLocalScale(2f);



//         Charles's Scene:
//        assetManager.registerLocator("Scenes/FPSCity.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("Models/FPSarena2.0.scene");
        int size = 513;
        float normalizer = 20.0f;
        float waterLevel = 0;
        Material mat = initMaterial(size, waterLevel, normalizer);
        sceneModel.setMaterial(mat);
        sceneModel.setLocalTranslation(0.0f, 20.0f, -87f);
        sceneModel.setLocalScale(0.7f);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape((Node) sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(4f, 6f, 1); // Originally: (1.5f, 6f, 1)
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);
        resetPlayerLocation();

        // We attach the scene and the player to the rootNode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);
        physicsSpace.add(landscape);
        physicsSpace.add(player);

        initHitMarker();

        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);

        shootables.attachChild(sceneModel);

//        golem_phy = new RigidBodyControl(0.0f);
//        ninja_phy = new RigidBodyControl(0.0f);
//        car_phy = new RigidBodyControl(0.0f);

        // TODO: When enemies are too far from player, save their location, remove them from rootNode to reduce lag, and put them back in using the saved location when it's close again.

        golemSpatial = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        golemGeom = getGeomFromSpatial(golemSpatial);

        golemSpatial2 = golemSpatial.clone();
        golemGeom2 = getGeomFromSpatial(golemSpatial2);

        golemSpatial3 = golemSpatial.clone();
        golemGeom3 = getGeomFromSpatial(golemSpatial3);

        golemSpatial4 = golemSpatial.clone();
        golemGeom4 = getGeomFromSpatial(golemSpatial4);

        golemSpatial5 = golemSpatial.clone();
        golemGeom5 = getGeomFromSpatial(golemSpatial5);

//        golemAnimControl = golemSpatial.getControl(AnimControl.class);
//        control.addListener(this);
//        golemAnimChannel = golemAnimControl.createChannel();
//        channel.setAnim("Walk");

        rootNode.attachChild(golemSpatial);
        rootNode.attachChild(golemSpatial2);
        rootNode.attachChild(golemSpatial3);
        rootNode.attachChild(golemSpatial4);
        rootNode.attachChild(golemSpatial5);


//        Spatial model = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
////        model.center();
//
//        control = model.getControl(AnimControl.class);
//
//        Box b = new Box(.25f,3f,.25f);
//        Geometry item = new Geometry("Item", b);
//        item.move(0, 1.5f, 0);
//        item.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
//        Node n = skeletonControl.getAttachmentsNode("hand.right");
//        n.attachChild(item);
//
//        rootNode.attachChild(model);







//        ninjaSpatial = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
//        ninjaGeom = getGeomFromSpatial(ninjaSpatial);
//        ninjaGeom.setLocalScale(0.1f, 0.1f, 0.1f);

        hoverTankSpatial = assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        hoverTankGeom = getGeomFromSpatial(hoverTankSpatial);

        hoverTankSpatial2 = hoverTankSpatial.clone();
        hoverTankGeom2 = getGeomFromSpatial(hoverTankSpatial2);

        hoverTankSpatial3 = hoverTankSpatial.clone();
        hoverTankGeom3 = getGeomFromSpatial(hoverTankSpatial3);


        hoverTankGeom.setLocalScale(3f, 3f, 3f);
        hoverTankGeom2.setLocalScale(3f, 3f, 3f);
        hoverTankGeom3.setLocalScale(3f, 3f, 3f);

//        rockshooterSpatial = assetManager.loadModel("Models/BRSchariotsMARY/BRSchariotsMARY.j3o");
//        rockshooterGeom = getGeomFromSpatial(rockshooterSpatial);


//        house = assetManager.loadModel("Models/house.mesh.xml");

        //stairs = assetManager.loadModel("Models/tileable stairs.j3o");

        weapon = assetManager.loadModel("Models/portal gun 2.j3o");

//        weapon_phy = new RigidBodyControl(0.0f);
//        weapon.addControl(weapon_phy);
//        physicsSpace.add(weapon_phy);

        rootNode.attachChild(weapon);
        //rootNode.attachChild(stairs);

        //stairs.setLocalTranslation(0, 100, 0);

//        house.setLocalTranslation(75, 2, 15);
//        house.setLocalScale(30f,30f,30f);


//        golem.addControl(golem_phy);
//        ninja.addControl(ninja_phy);
//        car.addControl(car_phy);


//        physicsSpace.add(golem_phy);
//        physicsSpace.add(ninja_phy);
//        physicsSpace.add(car_phy);



        enemyGeoms.add(golemGeom);
        enemyGeoms.add(golemGeom2);
        enemyGeoms.add(golemGeom3);
        enemyGeoms.add(golemGeom4);
        enemyGeoms.add(golemGeom5);

//        enemies.add(ninjaGeom);
        enemyGeoms.add(hoverTankGeom);
        enemyGeoms.add(hoverTankGeom2);
        enemyGeoms.add(hoverTankGeom3);


        initEnemies();

        initAI();

        createFlame();
        createFlash();
        createSpark();
        createRoundSpark();
        createSmokeTrail();
        createDebris();
        createShockwave();

        explosionEffect.setLocalScale(1f);
        renderManager.preloadScene(explosionEffect);

        rootNode.attachChild(explosionEffect);

        laserCylinder = new Cylinder(10, 20, 0.05f, 100f, true);
        laserBeam = new Geometry("Cylinder", laserCylinder);
        laserBeamOrigScale = laserBeam.getLocalScale().clone(); // For shrink/expand animations.
        laserBeam.setLocalScale(0f, 0f, 0f); // For shrink/expand animations.
        Material laserMaterial = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        laserMaterial.setColor("Color", ColorRGBA.Red);
        laserMaterial.setColor("GlowColor", ColorRGBA.Red);
        fpp = new FilterPostProcessor(assetManager);
        bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);

        laserBeam.setMaterial(laserMaterial);

        enemyBombMat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        enemyBombMat.setColor("Color", ColorRGBA.Blue);
        enemyBombMat.setColor("GlowColor", ColorRGBA.Blue);
        fpp = new FilterPostProcessor(assetManager);
        bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);

        // Start by assuming kinect skeleton is deactivated.
        // updateKinectJointsActions() will take care of updating the status.
        // NOTE: Keep this at the very end of simpleInitApp(), since it modifies
        // some stuff needing to be already initialized, such as the AIs.
        onKinectSkeletonDeactivated();
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    /**
     * We over-write some navigational key mappings here, so we can add
     * physics-controlled walking and jumping:
     */
    private void setUpKeys() {
        Map<String, Integer> keyboardInputMappings = new HashMap<String, Integer>() {
            {
                put("Left", KeyInput.KEY_A);
                put("Right", KeyInput.KEY_D);
                put("Up", KeyInput.KEY_W);
                put("Down", KeyInput.KEY_S);
                put("shoot", KeyInput.KEY_SPACE);
                put("enemyShoot", KeyInput.KEY_X);
                put("Jump", KeyInput.KEY_LCONTROL);
            }
        };

        Map<String, Integer> mouseInputMappings = new HashMap<String, Integer>() {
            {
                put("shoot", MouseInput.BUTTON_LEFT);
            }
        };

//        inputManager.addMapping("shoot",
//                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
//
//        inputManager.addListener(actionListener, "shoot");

        for (Entry<String, Integer> curKeyInputMapping : keyboardInputMappings.entrySet()) {
            String curKeyInputMappingName = curKeyInputMapping.getKey();
            inputManager.addMapping(curKeyInputMappingName, new KeyTrigger(curKeyInputMapping.getValue()));
            inputManager.addListener(this, curKeyInputMappingName);
        }

        for (Entry<String, Integer> curMouseInputMapping : mouseInputMappings.entrySet()) {
            String curMouseInputMappingName = curMouseInputMapping.getKey();
            inputManager.addMapping(curMouseInputMappingName, new MouseButtonTrigger(curMouseInputMapping.getValue()));
            inputManager.addListener(analogListener, curMouseInputMappingName);
        }

//            inputManager.addListener(analogListener, new String[]{"shoot"});

//        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
//        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
//        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
//        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
//        inputManager.addMapping("shoot", new KeyTrigger(KeyInput.KEY_SPACE));
//        inputManager.addMapping("enemyShoot", new KeyTrigger(KeyInput.KEY_X));
//        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_LCONTROL));
//        
//        inputManager.addListener(this, new String[]{"Left", "Right", "Up", "Down", "Jump", "shoot", "enemyShoot"});

    }
    // TODO: Currently user can use the mouse scroll wheel to zoom in and out and even see the whole gun! Replace this scroll (and even right click) controls to even help kinect controls (where user can carry wireless mouse while using Kinect!)

    // TODO: Why are there 2 onAction methods, both implementing the same com.jme3.input.controls.ActionListener ???
    // TODO: These key/mouse input-to-action bindings should use enum and switch constructs, for cleaner code.
    /**
     * These are our custom actions triggered by key presses. We do not walk
     * yet, we just keep track of the direction the user pressed.
     */
    public void onAction(String binding, boolean keyPressed, float tpf) {
        if (binding.equals("Left")) {
            inputIsMoveLeft = keyPressed;
        } else if (binding.equals("Right")) {
            inputIsMoveRight = keyPressed;
        } else if (binding.equals("Up")) {
            inputIsMoveUp = keyPressed;
        } else if (binding.equals("Down")) {
            inputIsMoveDown = keyPressed;
        } else if (binding.equals("Jump") && keyPressed) {
            player.jump();
        } else if (binding.equals("shoot")) {
            inputIsShooting = keyPressed;
            shoot();
        } else if (binding.equals("enemyShoot") && !keyPressed) {
            makeCannonBall();
        }
    }
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot")) {
                if (!keyPressed) {
                    inputIsShooting = false;

                } else {
                    inputIsShooting = true;
                                        audio_gun.playInstance();

                }
            } else if (name.equals("enemyShoot") && !keyPressed) {
                makeCannonBall();
            }
//            if (name.equals("shoot") && !keyPressed) {
//                shootRay();
//            }
        }
    };
    /**
     * Use this listener for continuous events
     */
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if (inputIsShooting) {
                if (name.equals("shoot")) {
                    shoot();
//                    audio_gun.playInstance();
//                    rootNode.attachChild(laserBeam);
//                    laserBeam.setLocalTranslation(weapon.getLocalTranslation());
//                    laserBeam.setLocalRotation(cam.getRotation());
                }
            } else {
//                laserBeam.removeFromParent();
//                System.out.println("Press P to unpause.");
            }
        }
    };

    // TODO: Maybe move entire HUD stuff into its own HUD class,
    // passing guiNode as constructor parameter.
    // But then needs a way to be updated in simpleUpdate here too.
    // TODO: Also move enemies/shootables to their own class maybe, etc.
    /**
     * Initialize Heads-Up Display (HUD) that contains objects that follow the
     * player's screen and game state information, such as crosshair, time
     * remaining, accumulated score so far, etc. [This is to be called once in
     * simpleInitApp()]
     */
    private void initHUD() {
        /* Clear default items first */
        setDisplayStatView(false);
//        setDisplayFps(false);
        guiNode.detachAllChildren();

        /* Initialize custom items */
        initCrossHairs();

        initHUDTimer();

        initHUDScore();

        initHUDEnemyCounter();

        initGoalText();

        initKinectControlsStatusText();
    }

    /**
     * Update objects on the Heads-Up Display (HUD) that need real-time updates,
     * such as crosshair, time remaining, and accumulated score so far, etc.
     * [This is to be called in simpleUpdate()]
     */
    private void updateHUD() {
        updateHUDTimerText();
        // Because timer location is currently set on top left and content
        // grows towards the right, no need to:
        // (but keep it for when we want to change default location)
//        updateHUDTimerLocation();

        updateHUDScoreText();
        // Because timer location is currently set on top right and content
        // grows towards the right, do need to:
        updateHUDScoreLocation();

        updateHUDEnemyCounterText();
        // Because enemy counter location is currently set on top left and content
        // grows towards the right, no need to:
        // (but keep it for when we want to change default location)
        updateHUDEnemyCounterLocation();
    }

    // TODO: Should be only one init function that accepts parameters to initialize all kinds of HUD items.
    private void initHUDTimer() {
        hudTimer = new BitmapText(guiFont, false);
        hudTimer.setSize(guiFont.getCharSet().getRenderedSize() * hudTimerFontSizeMultiplier);      // font size
        hudTimer.setColor(ColorRGBA.Blue);                             // font color
//        hudTimer.setShadowMode(RenderQueue.ShadowMode.Cast);

        updateHUDTimerText();
        updateHUDTimerLocation();

        guiNode.attachChild(hudTimer);
    }

    private void updateHUDTimerText() {
        hudTimer.setText("Time: " + toClockFormat(remainingRoundTimeSec));
    }

    /* Especially used after text content has been edited, since text length
     * may change and position may need to be recalculated.
     */
    private void updateHUDTimerLocation() {
        // Center timer: Half of screen width minus half of text's line width
//        float centerPos = (settings.getWidth()/2) - (hudTimer.getLineWidth()/2);
        float leftCornerPos = 0f,
                topCornerPos = settings.getHeight();

        hudTimer.setLocalTranslation(leftCornerPos, topCornerPos, 0f); // position
    }

    private void initHUDEnemyCounter() {
        hudEnemyCounter = new BitmapText(guiFont, false);
        hudEnemyCounter.setSize(guiFont.getCharSet().getRenderedSize() * hudEnemyCounterFontSizeMultiplier);      // font size
        hudEnemyCounter.setColor(ColorRGBA.Red);                             // font color

        updateHUDEnemyCounterText();
        updateHUDEnemyCounterLocation();

        guiNode.attachChild(hudEnemyCounter);
    }

    private void updateHUDEnemyCounterText() {
        hudEnemyCounter.setText("Enemies Left: " + getNumberOfAliveEnemies());
    }

    private void updateHUDEnemyCounterLocation() {
        // Center: Half of screen width minus half of text's line width
        float centerPos = (settings.getWidth() / 2) - (hudEnemyCounter.getLineWidth() / 2),
                topCornerPos = settings.getHeight();

        hudEnemyCounter.setLocalTranslation(centerPos, topCornerPos, 0f); // position
    }

    private void initHUDScore() {
        hudScore = new BitmapText(guiFont, false);
        hudScore.setSize(guiFont.getCharSet().getRenderedSize() * hudScoreFontSizeMultiplier);      // font size
        hudScore.setColor(ColorRGBA.Green);                             // font color

        updateHUDScoreText();
        updateHUDScoreLocation();

        guiNode.attachChild(hudScore);
    }

    private void updateHUDScoreText() {
        hudScore.setText("Score: " + totalScore);
    }

    /* Especially used after text content has been edited, since text length
     * may change and position may need to be recalculated.
     */
    private void updateHUDScoreLocation() {
        float rightCornerPos = settings.getWidth(),
                topCornerPos = settings.getHeight();

        hudScore.setLocalTranslation(rightCornerPos - hudScore.getLineWidth(), topCornerPos, 0f); // position
    }
    private BitmapText goalText = null;

    private void initGoalText() {
        goalText = new BitmapText(guiFont, false);
        goalText.setSize(guiFont.getCharSet().getRenderedSize() * hudGoalFontSizeMultiplier);      // font size
        goalText.setColor(ColorRGBA.Red);                             // font color

        // Don't attach to guiNode as to not show the text right away
    }

    private void showGoalText() {
        // Center: Half of screen width minus half of text's line width
        float centerPos = (settings.getWidth() / 2) - (goalText.getLineWidth() / 2);
        // Middle: Half of screen height plus half of text's line height
        float middlePos = (settings.getHeight() / 2) + (goalText.getLineHeight() / 2);

        goalText.setLocalTranslation(centerPos, middlePos, 0f); // position

        guiNode.attachChild(goalText);
    }

    private void hideGoalText() {
        guiNode.detachChild(goalText);
    }
    private BitmapText kinectControlsStatusText = null;
    private Geometry kinectControlsStatusTextBackground = null;

    private void initKinectControlsStatusText() {
        kinectControlsStatusText = new BitmapText(guiFont, false);
        kinectControlsStatusText.setSize(guiFont.getCharSet().getRenderedSize() * hudKinectControlsStatusFontSizeMultiplier);      // font size
        kinectControlsStatusText.setColor(ColorRGBA.Yellow);                             // font color

        kinectControlsStatusTextBackgroundMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        kinectControlsStatusTextBackgroundMaterial.setColor("Color", new ColorRGBA(0, 0, 0, 0.5f));
        kinectControlsStatusTextBackgroundMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        kinectControlsStatusTextBackground = new Geometry("KinectControlsStatusTextBackground");
        kinectControlsStatusTextBackground.setMaterial(kinectControlsStatusTextBackgroundMaterial);

        // Don't attach text & background to guiNode as to not show the text right away
    }

    private void showKinectControlsStatusText() {

        float fpsTextLineHeight = fpsText.getLineHeight(),
                kinectControlsStatusTextLineWidth = kinectControlsStatusText.getLineWidth(),
                kinectControlsStatusTextLineHeight = kinectControlsStatusText.getLineHeight();

        kinectControlsStatusTextBackground.setMesh(new Quad(kinectControlsStatusTextLineWidth, kinectControlsStatusTextLineHeight));
        kinectControlsStatusTextBackground.setLocalTranslation(0f, fpsTextLineHeight, 0f);

        kinectControlsStatusText.setLocalTranslation(0f, fpsTextLineHeight + kinectControlsStatusTextLineHeight, 0f); // position

        guiNode.attachChild(kinectControlsStatusTextBackground);
        guiNode.attachChild(kinectControlsStatusText);
    }

    private void hideKinectControlsStatusText() {
        guiNode.detachChild(kinectControlsStatusText);
        guiNode.detachChild(kinectControlsStatusTextBackground);
    }

    private String toClockFormat(long seconds, String separator) {
        int days = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long secondsRemain = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        StringBuilder strClockFormat = new StringBuilder();

        if (days > 0) {
            strClockFormat.append(days).append(separator);
        }

        if (hours > 0) {
            if (hours < 10) {
                strClockFormat.append("0");
            }
            strClockFormat.append(hours).append(separator);
        }

        // Minutes and seconds always visible no matter what
        if (minutes < 10) {
            strClockFormat.append("0");
        }
        strClockFormat.append(minutes).append(separator);

        if (secondsRemain < 10) {
            strClockFormat.append("0");
        }
        strClockFormat.append(secondsRemain);

        return strClockFormat.toString();
    }

    private String toClockFormat(long seconds) {
        return toClockFormat(seconds, ":");
    }

    private void decrementRemainingRoundTime() {
        if (remainingRoundTimeSec > 0) {
            remainingRoundTimeSec--;
        } else {
            lose();
        }
    }

    /**
     * A plus sign used as crosshairs to help the player with aiming.
     */
    private void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        crosshair = new BitmapText(guiFont, false);
        crosshair.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        crosshair.setText("+");        // fake crosshairs :)
        crosshair.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + crosshair.getLineHeight() / 2, 0);
        guiNode.attachChild(crosshair);
    }

    private void initHitMarker() {
        Sphere sphere = new Sphere(30, 30, 0.2f);
        hitMarker = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        hitMarker.setMaterial(mark_mat);
    }

    private void shoot() {

        if (gameOver) {
            return; // Don't shoot at all
        }

        // Comment code below if not want to enforce fire rate
        if (!allowedToFire()) {
            return;
        }

        // 5. Use the results (we mark the hit object)
        if (aShootableIsWithinPlayerAim()) {

            // 4. Print the results
//                System.out.println("----- Collisions? " + results.size() + "-----");
            //            for (int i = 0; i < results.size(); i++) {
            // For each hit, we know distance, impact point, name of geometry.
//            CollisionResult curCollision = results.getCollision(i);

            // The closest collision point is what was truly hit:
            CollisionResult closestCollision = getCollisionResultOfClosestShootableWithinPlayerAim();
            Vector3f hitLocation = closestCollision.getContactPoint();
            float dist = closestCollision.getDistance();
            Vector3f pt = closestCollision.getContactPoint();
            Geometry curHitGeom = closestCollision.getGeometry();
            String curHitName = curHitGeom.getName();
            // Let's interact - we mark the hit with a red dot.
            hitMarker.setLocalTranslation(hitLocation);
            showHitMarker();

//                    System.out.println("* Collision #" + 0);
//            System.out.println("  You shot " + curHitName + " at " + pt + ", " + dist + " wu away.");

            // curHitGeom.getParent() != null means
            // curHitGeom's parent was removed before already;
            // ignore it to prevent redundant collision-handling
            // TODO: May need to change the way to prevent duplicate collison-handling to be more efficient
            if (dist <= maxShootRange && curHitGeom.getParent() != null && enemyGeoms.contains(curHitGeom)) {
                long scoreBonus = 0; // Award bonuses depending on how the enemy is shot

                int newCurHitEnemyHP = enemyHitPoints.get(curHitGeom) - 1;
                enemyHitPoints.put(curHitGeom, newCurHitEnemyHP);
                audio_impact.playInstance();

//                            System.out.println("Hit Enemy Name = "+curHitName);
//                            System.out.println("Hit Enemy's Parent = "+curHitGeom.getParent());

                boolean hitIsADodgingAI = dodgingAIs.keySet().contains(curHitGeom);
                if (hitIsADodgingAI) {
                    // Force dodging AI to dodge for a certain time!
                    dodgingAIs.get(curHitGeom).forceDodge();

//                    if (!channel.getAnimationName().equals("Walk")) {
//                        channel.setAnim("Walk", 0.50f);
//                        channel.setLoopMode(LoopMode.Loop);
//                    }
                    // TODO: Detect how fast targets were moving when hit, then determine a bonus score.
                }

                boolean hitIsADodgeShooterAI = dodgeShooterAIs.keySet().contains(curHitGeom);

                if (newCurHitEnemyHP <= 0) { // Enemy is dead
                    disableRigidBodyControl(curHitGeom);
                    curHitGeom.removeFromParent();
                    hideHitMarker();
                    // Previously: createExplosion(curHitGeom.getLocalTranslation())
                    createExplosion(hitLocation);
                    audio_explode.playInstance();


                    /* Bonus for destroying */
                    if (hitIsADodgingAI) {
                        scoreBonus += movingTargetsDestroyScoreBonus;
                    } else {
                        scoreBonus += stationaryTargetsDestroyScoreBonus;
                    }

                    if (hitIsADodgeShooterAI) {
                        scoreBonus += attackingTargetsExtraDestroyScoreBonus;
                    }

                    /* Bonus for speediness in destroying */
                    long elapsedTimeSec = DEFAULT_ROUND_TIME_LIMIT_SECONDS - remainingRoundTimeSec;
                    if (elapsedTimeSec < (.05 * DEFAULT_ROUND_TIME_LIMIT_SECONDS)) { // >=95% time remaining
                        scoreBonus += quickestDestroyScoreBonus;
                    } else if (elapsedTimeSec < (.5 * DEFAULT_ROUND_TIME_LIMIT_SECONDS)) { // >=50% time remaining
                        scoreBonus += quickDestroyScoreBonus;
                    }
                } else { // Enemy still alive
                                    /* Bonus for hitting */
                    if (hitIsADodgingAI) {
                        scoreBonus += movingTargetsHitScoreBonus;
                    } else {
                        scoreBonus += stationaryTargetsHitScoreBonus;
                    }
                }
//                            System.out.println("Hit Enemy's Parent = "+curHitGeom.getParent());
                addToTotalScore(scoreBonus);
            }

            if (getNumberOfAliveEnemies() <= 0) {
                win();
            }
        } else { // No hits? Then remove the red mark.
            hideHitMarker();
        }
    }

    private int getNumberOfAliveEnemies() {
        if (shootables != null) {
            return shootables.getQuantity() - 1; // 1 is left for the scenery
        } else {
            return 0;
        }
    }

    private void updatePlayerWalkingCamera() {
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        if (inputIsMoveLeft) {
            walkDirection.addLocal(camLeft);
        }
        if (inputIsMoveRight) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (inputIsMoveUp) {
            walkDirection.addLocal(new Vector3f(cam.getDirection().getX(), 0, cam.getDirection().getZ()).multLocal(0.4f));
        }
        if (inputIsMoveDown) {
            walkDirection.addLocal(new Vector3f(-cam.getDirection().getX(), 0, -cam.getDirection().getZ()).multLocal(0.4f));
        }

        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }
    public static final int KINECT_JOINT_POSITIONS_OFFSET_MILLIMETERS = 32768;

    private void updateKinectJointsActions() {
        if (!ENABLE_KINECT_INPUT || kinect == null) {
            return;
        }

        joints = kinect.getJoints();

//        if (joints != null) {
//            // Readings' units are in millimeters (mm)
//            rightShoulderY = joints[SHOULDER_RIGHT][2];
//            rightHandY = joints[HAND_RIGHT][2];
//            
//            cam.setLocation(new Vector3f(cam.getLocation().x,(float)rightHand[2],(float)rightHand[3]));
//            
////            // Shoot: if right hand raised above right elbow, plus extra height
////            int extraRaisedHeight = 115, // About the height of iPhone 4 (115mm / 4.5in)
////            minRaisedHandHeight = rightElbowY+extraRaisedHeight;
////            
////            if (rightHandY > minRaisedHandHeight){
////                shootRay();
////            }
//        }

        if (joints != null) {
            if (!kinectSkeletonActive) {
                // Kinect skeleton reading has just become available,
                // when the readings are previously NOT available.
                onKinectSkeletonActivated(); // Invoke listener
            }

            // Save needed readings to buffers
            spine = joints[SPINE];
            spineX = spine[KJ_X];
            spineY = spine[KJ_Y];
            spineZ = spine[KJ_Z];

            leftShoulder = joints[SHOULDER_LEFT];
            leftShoulderX = leftShoulder[KJ_X];
            leftShoulderY = leftShoulder[KJ_Y];
            leftShoulderZ = leftShoulder[KJ_Z];

            leftWrist = joints[WRIST_LEFT];
            leftWristX = leftWrist[KJ_X];
            leftWristY = leftWrist[KJ_Y];
            leftWristZ = leftWrist[KJ_Z];

            centerShoulder = joints[SHOULDER_CENTER];
            centerShoulderX = centerShoulder[KJ_X];
            centerShoulderY = centerShoulder[KJ_Y];
            centerShoulderZ = centerShoulder[KJ_Z];

            rightShoulder = joints[SHOULDER_RIGHT];
            rightShoulderX = rightShoulder[KJ_X];
            rightShoulderY = rightShoulder[KJ_Y];
            rightShoulderZ = rightShoulder[KJ_Z];

            rightWrist = joints[WRIST_RIGHT];
            rightWristX = rightWrist[KJ_X];
            rightWristY = rightWrist[KJ_Y];
            rightWristZ = rightWrist[KJ_Z];

//            float rightHandX = ((float) (rightWrist[1])),// * kinectLookUpDownMultiplier,
//                    rightHandY = ((float) (rightWrist[2])),// * kinectLookUpDownMultiplier,
//                    rightHandZ = ((float) -(rightWrist[3]));// * kinectTurnLeftRightMultiplier;  // Negated reading to get matching turn left/right directions
//System.out.println(rightHandZ);
//            System.out.println("rightHandX = "+rightHandX+" | rightHandY = "+rightHandY+" | rightHandZ = "+rightHandZ);

            Vector3f curCamDir = cam.getDirection();

            int xRightWristOffsetFromRightShoulder = (rightWristX - rightShoulderX), // *kinectTurnLeftRightMultiplier
                    yRightWristOffsetFromRightShoulder = (rightWristY - rightShoulderY),
                    zRightWristOffsetFromRightShoulder = (rightWristZ - rightShoulderZ);

            Vector3f kinectLoc = new Vector3f(curCamDir.x + xRightWristOffsetFromRightShoulder, curCamDir.y + yRightWristOffsetFromRightShoulder, curCamDir.z + zRightWristOffsetFromRightShoulder);

//                    hitMarker.setLocalTranslation(newLoc);
//                    showHitMarker();

            int xRightWristOffsetFromCenterShoulder = (rightWristX - centerShoulderX),
                    yRightWristOffsetFromCenterShoulder = (rightWristY - centerShoulderY),
                    zRightWristOffsetFromCenterShoulder = (rightWristZ - centerShoulderZ);

//            int xRightWristOffsetFromSpine = (rightWristX-spineX),
//                yRightWristOffsetFromSpine = (rightWristY-spineY),
//                zRightWristOffsetFromSpine = (rightWristZ-spineZ);

            if (Math.abs(xRightWristOffsetFromCenterShoulder) <= 50 /*&&
                     Math.abs(yRightWristOffsetFromCenterShoulder) <= 50 &&
                     Math.abs(zRightWristOffsetFromCenterShoulder) <= 50*/) {
                // Add turn left (aim) velocity
//                    cam.lookAtDirection(kinectLoc.addLocal(kinectLoc), Vector3f.UNIT_Y);
            }
//                else if (Math.abs(xRightWristOffsetFromRightShoulder) <= 50 &&
//                         Math.abs(yRightWristOffsetFromSpine) <= 50 &&
//                         Math.abs(zRightWristOffsetFromRightShoulder) <= 50){
//                    shoot();
//                }

            cam.lookAtDirection(kinectLoc, Vector3f.UNIT_Y);
        } else {
            if (kinectSkeletonActive) {
                // Kinect skeleton reading has just become NOT available,
                // when the readings are previously available.
                onKinectSkeletonDeactivated(); // Invoke listener
            }
        }
    }
    /**
     * This is the main event loop--walking happens here. We check in which
     * direction the player is walking by interpreting the camera direction
     * forward (camDir) and to the side (camLeft). The setWalkDirection()
     * command is what lets a physics-controlled player walk. We also make sure
     * here that the camera moves with player.
     */
    private float secondCounter;
    /**
     * Global copy of current tpf value from simpleUpdate. This will mainly be
     * used to sync object transformations (trans./rot./scale) across machines
     * with different CPU speeds.
     */
    private float tpf = 1;

    @Override
    public void simpleUpdate(float tpf) {
        this.tpf = tpf; // Save to global copy

        secondCounter += tpf; // Used to calculate 1 second from tpf
        if (secondCounter >= 1f) { // Program enters here every 1 second
            secondCounter = 0; // Used to calculate 1 second from tpf (reset)
            oneSecondUpdates();
        }

//        This can change camera look direction smoothly/animated!
//        cam.lookAtDirection(cam.getDirection().interpolate(cam.getDirection().add(1, 1, 1), .001f), Vector3f.UNIT_Y);

//        System.out.println(golemHP);

        updateHUD();

        updateCollisionResultsOfShootablesWithinPlayerAim();

        updateHitMarker();

        updatePlayerWalkingCamera();

        updateKinectJointsActions();

        makeEnemiesDodgePlayerAim();
        updateDodgeShooterAIs();

        loadWeapon();
        loadLaser();
    }
    private int restartGameOverTimer = 0;

    /**
     * Place stuff to update every one second in here
     */
    private void oneSecondUpdates() {
        if (!gameOver) {
            decrementRemainingRoundTime();
//            System.out.println(cam.getLocation().y+" | "+sceneModel.getLocalTranslation().y+" | "+sceneModel.getLocalScale().y);
            // Player fell off the ground!
            if (cam.getLocation().y < MIN_PLAYABLE_LOCATION_Y_AXIS) { // cliffBelow is the best dynamic checking for now. However, this will cause player to lose if it's very near the edge or even if it's standing above a small gap.
                if (cam.getLocation().y < MIN_PLAYABLE_LOCATION_Y_AXIS) // cliffBelow is the best dynamic checking for now. However, this will cause player to lose if it's very near the edge.
                {
                    lose();
                }
            }
        } else {
            restartGameOverTimer++;
            toggleVisibility(goalText, guiNode);

            if (restartGameOverTimer == 5) { // Wait for 5 seconds
                restartGameOverTimer = 0; // Reset

                restartGame();
            }
        }
    }

    private void win() {
        addToTotalScore(winGameScoreBonus);
        goalText.setColor(ColorRGBA.Green);
        goalText.setText("YOU WIN!");
        showGoalText();
        audio_win.playInstance();
        gameOver = true;
    }

    private void lose() {
        resetTotalScore();
        goalText.setColor(ColorRGBA.Red);
        goalText.setText("YOU LOSE!");
        showGoalText();
        audio_lose.playInstance();
        gameOver = true;
    }

    private void restartGame() {
        uninitEnemies();
        resetPlayerLocation();
        initEnemies();
        resetSettings();
    }

    private void enableRigidBodyControl(Geometry targetObject) {
        RigidBodyControl RBC = targetObject.getControl(RigidBodyControl.class);
        if (RBC == null) {
            RBC = new RigidBodyControl(0.0f);
            targetObject.addControl(RBC);
            physicsSpace.add(RBC);
        } else {
            RBC.setEnabled(true);
        }
    }

    private void disableRigidBodyControl(Geometry targetObject) {
        RigidBodyControl RBC = targetObject.getControl(RigidBodyControl.class);
        if (RBC != null) {
            RBC.setEnabled(false);
//                targetObject.removeControl(RBC);
//                physicsSpace.remove(RBC);
        }
    }

    private void initEnemies() {
//        golemGeom.setLocalTranslation(0, 5, 0);
//        golemGeom2.setLocalTranslation(50, 5, 0);
//        golemGeom3.setLocalTranslation(25, 5, -25);
////        ninjaGeom.setLocalTranslation(50, 0, 0);
//        hoverTankGeom.setLocalTranslation(-20, 50, 0);
////        rockshooter.setLocalTranslation(75, 5, 0);

        //Charles City
        golemGeom.setLocalTranslation(45, -15, 120);
        golemGeom2.setLocalTranslation(-90, -15, 50);
        golemGeom3.setLocalTranslation(-70, -15, 135);
        golemGeom4.setLocalTranslation(12, -15, -41);
        golemGeom5.setLocalTranslation(10, -85, 215);


        hoverTankGeom.setLocalTranslation(-250, -50, 250);
        hoverTankGeom2.setLocalTranslation(-22, 50, 130);
        hoverTankGeom3.setLocalTranslation(2, 50, 220);

        // Reset all enemy hit points (health)
        for (Geometry curEnemy : enemyGeoms) {
            enemyHitPoints.put(curEnemy, DEFAULT_ENEMY_HIT_POINTS);
            enableRigidBodyControl(curEnemy);
            shootables.attachChild(curEnemy);
        }
    }

    private void uninitEnemies() {
        for (Geometry curEnemy : enemyGeoms) {
            disableRigidBodyControl(curEnemy);
            curEnemy.removeFromParent();
        }
    }

    private void loadWeapon() {
        Vector3f vectorDifference = new Vector3f(cam.getLocation().subtract(weapon.getWorldTranslation()));
        weapon.setLocalTranslation(vectorDifference.addLocal(weapon.getLocalTranslation()));

        Quaternion worldDiff = new Quaternion(cam.getRotation().mult(weapon.getWorldRotation().inverse()));
        weapon.setLocalRotation(worldDiff.multLocal(weapon.getLocalRotation()));

        // Move it to the bottom right of the screen
        weapon.move(cam.getDirection().mult(3));
        weapon.move(cam.getUp().mult(-0.8f));
        weapon.move(cam.getLeft().mult(-0.8f));

        weapon.setLocalScale(0.4f);
        weapon.rotate(0, -1.5f, 0);
    }

    private void createFlame() {
        flame = new ParticleEmitter("Flame", EMITTER_TYPE, 32 * COUNT_FACTOR);
        flame.setSelectRandomImage(true);
        flame.setStartColor(new ColorRGBA(1f, 0.4f, 0.05f, (float) (1f / COUNT_FACTOR_F)));
        flame.setEndColor(new ColorRGBA(.4f, .22f, .12f, 0f));
        flame.setStartSize(1.3f);
        flame.setEndSize(2f);
        flame.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
        flame.setParticlesPerSec(0);
        flame.setGravity(0, -5, 0);
        flame.setLowLife(.4f);
        flame.setHighLife(.5f);
        flame.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 7, 0));
        flame.getParticleInfluencer().setVelocityVariation(1f);
        flame.setImagesX(2);
        flame.setImagesY(2);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        flame.setMaterial(mat);
        explosionEffect.attachChild(flame);
    }

    private void createFlash() {
        flash = new ParticleEmitter("Flash", EMITTER_TYPE, 24 * COUNT_FACTOR);
        flash.setSelectRandomImage(true);
        flash.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, (float) (1f / COUNT_FACTOR_F)));
        flash.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        flash.setStartSize(.1f);
        flash.setEndSize(3.0f);
        flash.setShape(new EmitterSphereShape(Vector3f.ZERO, .05f));
        flash.setParticlesPerSec(0);
        flash.setGravity(0, 0, 0);
        flash.setLowLife(.2f);
        flash.setHighLife(.2f);
        flash.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 5f, 0));
        flash.getParticleInfluencer().setVelocityVariation(1);
        flash.setImagesX(2);
        flash.setImagesY(2);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flash.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        flash.setMaterial(mat);
        explosionEffect.attachChild(flash);
    }

    private void createRoundSpark() {
        roundspark = new ParticleEmitter("RoundSpark", EMITTER_TYPE, 20 * COUNT_FACTOR);
        roundspark.setStartColor(new ColorRGBA(1f, 0.29f, 0.34f, (float) (1.0 / COUNT_FACTOR_F)));
        roundspark.setEndColor(new ColorRGBA(0, 0, 0, (float) (0.5f / COUNT_FACTOR_F)));
        roundspark.setStartSize(1.2f);
        roundspark.setEndSize(1.8f);
        roundspark.setShape(new EmitterSphereShape(Vector3f.ZERO, 2f));
        roundspark.setParticlesPerSec(0);
        roundspark.setGravity(0, -.5f, 0);
        roundspark.setLowLife(1.8f);
        roundspark.setHighLife(2f);
        roundspark.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 3, 0));
        roundspark.getParticleInfluencer().setVelocityVariation(.5f);
        roundspark.setImagesX(1);
        roundspark.setImagesY(1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/roundspark.png"));
        mat.setBoolean("PointSprite", POINT_SPRITE);
        roundspark.setMaterial(mat);
        explosionEffect.attachChild(roundspark);
    }

    private void createSpark() {
        spark = new ParticleEmitter("Spark", ParticleMesh.Type.Triangle, 30 * COUNT_FACTOR);
        spark.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, (float) (1.0f / COUNT_FACTOR_F)));
        spark.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        spark.setStartSize(.5f);
        spark.setEndSize(.5f);
        spark.setFacingVelocity(true);
        spark.setParticlesPerSec(0);
        spark.setGravity(0, 5, 0);
        spark.setLowLife(1.1f);
        spark.setHighLife(1.5f);
        spark.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 20, 0));
        spark.getParticleInfluencer().setVelocityVariation(1);
        spark.setImagesX(1);
        spark.setImagesY(1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/spark.png"));
        spark.setMaterial(mat);
        explosionEffect.attachChild(spark);
    }

    private void createSmokeTrail() {
        smoketrail = new ParticleEmitter("SmokeTrail", ParticleMesh.Type.Triangle, 22 * COUNT_FACTOR);
        smoketrail.setStartColor(new ColorRGBA(1f, 0.8f, 0.36f, (float) (1.0f / COUNT_FACTOR_F)));
        smoketrail.setEndColor(new ColorRGBA(1f, 0.8f, 0.36f, 0f));
        smoketrail.setStartSize(.2f);
        smoketrail.setEndSize(1f);

//        smoketrail.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
        smoketrail.setFacingVelocity(true);
        smoketrail.setParticlesPerSec(0);
        smoketrail.setGravity(0, 1, 0);
        smoketrail.setLowLife(.4f);
        smoketrail.setHighLife(.5f);
        smoketrail.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 12, 0));
        smoketrail.getParticleInfluencer().setVelocityVariation(1);
        smoketrail.setImagesX(1);
        smoketrail.setImagesY(3);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/smoketrail.png"));
        smoketrail.setMaterial(mat);
        explosionEffect.attachChild(smoketrail);
    }

    private void createDebris() {
        debris = new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 15 * COUNT_FACTOR);
        debris.setSelectRandomImage(true);
        debris.setRandomAngle(true);
        debris.setRotateSpeed(FastMath.TWO_PI * 4);
        debris.setStartColor(new ColorRGBA(1f, 0.59f, 0.28f, (float) (1.0f / COUNT_FACTOR_F)));
        debris.setEndColor(new ColorRGBA(.5f, 0.5f, 0.5f, 0f));
        debris.setStartSize(.2f);
        debris.setEndSize(.2f);

//        debris.setShape(new EmitterSphereShape(Vector3f.ZERO, .05f));
        debris.setParticlesPerSec(0);
        debris.setGravity(0, 12f, 0);
        debris.setLowLife(1.4f);
        debris.setHighLife(1.5f);
        debris.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 15, 0));
        debris.getParticleInfluencer().setVelocityVariation(.60f);
        debris.setImagesX(3);
        debris.setImagesY(3);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/Debris.png"));
        debris.setMaterial(mat);
        explosionEffect.attachChild(debris);
    }

    private void createShockwave() {
        shockwave = new ParticleEmitter("Shockwave", ParticleMesh.Type.Triangle, 1 * COUNT_FACTOR);
//        shockwave.setRandomAngle(true);
        shockwave.setFaceNormal(Vector3f.UNIT_Y);
        shockwave.setStartColor(new ColorRGBA(.48f, 0.17f, 0.01f, (float) (.8f / COUNT_FACTOR_F)));
        shockwave.setEndColor(new ColorRGBA(.48f, 0.17f, 0.01f, 0f));

        shockwave.setStartSize(0f);
        shockwave.setEndSize(7f);

        shockwave.setParticlesPerSec(0);
        shockwave.setGravity(0, 0, 0);
        shockwave.setLowLife(0.5f);
        shockwave.setHighLife(0.5f);
        shockwave.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0, 0));
        shockwave.getParticleInfluencer().setVelocityVariation(0f);
        shockwave.setImagesX(1);
        shockwave.setImagesY(1);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/shockwave.png"));
        shockwave.setMaterial(mat);
        explosionEffect.attachChild(shockwave);
    }

    private void createExplosion(Vector3f x) {

        flash.setLocalTranslation(x);
        spark.setLocalTranslation(x);
        smoketrail.setLocalTranslation(x);
        debris.setLocalTranslation(x);
        shockwave.setLocalTranslation(x);
        flame.setLocalTranslation(x);
        roundspark.setLocalTranslation(x);

        flash.emitAllParticles();
        spark.emitAllParticles();
        smoketrail.emitAllParticles();
        debris.emitAllParticles();
        shockwave.emitAllParticles();
        flame.emitAllParticles();
        roundspark.emitAllParticles();

    }

    private void loadLaser() {
//        if (allowedToFire(false)){
        positionLaserBeam();

        if (inputIsShooting) {
            expandLaserBeam();
        } else {
            shrinkLaserBeam();
        }
//        }
    }

    private void positionLaserBeam() {
        Vector3f weaponLoc = weapon.getLocalTranslation();
//        Vector3f laserBeamScale = laserBeam.getLocalScale();
        laserBeam.setLocalTranslation(weaponLoc.x, weaponLoc.y - 0.2f, weaponLoc.z);
//        laserBeam.setLocalScale();
        CollisionResult shootableWithinAim = getCollisionResultOfClosestShootableWithinPlayerAim();
        if (shootableWithinAim != null) {
            laserBeam.lookAt(shootableWithinAim.getContactPoint(), Vector3f.UNIT_Y);
//                // This caused out-of-memory error! updateGeometry is VERY EXPENSIVE!
////                float newLaserCylinderHeight = laserBeam.getLocalTranslation().distance(hitMarker.getLocalTranslation());
////                laserCylinder.updateGeometry(laserCylinder.getAxisSamples(), laserCylinder.getRadialSamples(),laserCylinder.getRadius(), laserCylinder.getRadius2(), newLaserCylinderHeight, laserCylinder.isClosed(), laserCylinder.isInverted());
        } else {
            laserBeam.setLocalRotation(cam.getRotation());
        }
    }

    private void expandLaserBeam() {
        rootNode.attachChild(laserBeam);
        Vector3f curScale = laserBeam.getLocalScale();
//                     curLoc = laserBeam.getLocalTranslation();

        float expandRate = LASER_BEAM_SHRINK_EXPAND_RATE * tpf;
        if (curScale.x < laserBeamOrigScale.x) {
            curScale.x += expandRate;
        } else {
            curScale.x = laserBeamOrigScale.x;
        }

        if (curScale.y < laserBeamOrigScale.y) {
            curScale.y += expandRate;
        } else {
            curScale.y = laserBeamOrigScale.y;
        }

        if (curScale.z < laserBeamOrigScale.z) {
            curScale.z += expandRate;
        } else {
            curScale.z = laserBeamOrigScale.z;
        }

        laserBeam.setLocalScale(curScale.x, curScale.y, curScale.z);
    }

    private void shrinkLaserBeam() {
        if (laserBeam.getParent() != null) {
            Vector3f curScale = laserBeam.getLocalScale();
//                     curLoc = laserBeam.getLocalTranslation();
            if (curScale.x <= 0 || curScale.y <= 0 || curScale.z <= 0) {
                laserBeam.removeFromParent();
            } else {
                float shrinkRate = LASER_BEAM_SHRINK_EXPAND_RATE * tpf;
//                    laserBeam.setLocalTranslation();
                laserBeam.setLocalScale(curScale.x - shrinkRate, curScale.y - shrinkRate, curScale.z - shrinkRate);
            }
        }
    }

    private void makeEnemiesDodgePlayerAim() {
        for (Entry<Geometry, DodgingAI> curDodgingAIEntry : dodgingAIs.entrySet()) {
            if (curDodgingAIEntry.getKey().getParent() != null) {
                curDodgingAIEntry.getValue().dodgeWhenAppropriate(tpf);
            }
        }
    }

    private void updateDodgeShooterAIs() {
        for (Entry<Geometry, DodgeShooterAI> curDodgeShooterAIEntry : dodgeShooterAIs.entrySet()) {
            if (curDodgeShooterAIEntry.getKey().getParent() != null) {
                curDodgeShooterAIEntry.getValue().dodgeShootWhenAppropriate(tpf);
            }
        }
    }

    /**
     * Provides a guarded way to add to total score by using the max limit.
     */
    private void addToTotalScore(long amount) {
        totalScore += amount;
        if (totalScore > MAX_TOTAL_SCORE) {
            totalScore = MAX_TOTAL_SCORE;
        }
    }

    /**
     * Provides a guarded way to add to total score by using the max limit.
     */
    private void incrementTotalScore() {
        addToTotalScore(1);
    }

    private void resetTotalScore() {
        totalScore = 0;
    }

    /**
     * Reset last shot time to effectively reset fire rate delay, so player may
     * fire immediately again.
     */
    private void resetFireRateDelay() {
        lastShotFiredTimeMillis = System.currentTimeMillis() - maxFireRateMillis - 1;
    }

    /**
     * Only allow player to shoot again if last shot occurred longer ago than
     * the maxFireRateMillis frequency. Code that uses this and about to really
     * fire should also update lastShotFiredTimeMillis when allowed to fire. As
     * a shortcut for updating, pass no parameter to this method or pass true,
     * and lastShotFiredTimeMillis will also be updated.
     *
     * @param alsoUpdateLastShotFiredTime when true, also updates
     * lastShotFiredTimeMillis. Should be true mainly for the primary shooting
     * mechanism only, and use allowedToFire(false) otherwise (e.g.: the laser
     * effect).
     */
    private boolean allowedToFire(boolean alsoUpdateLastShotFiredTime) {
        long currentTimeMillis = System.currentTimeMillis();
        if (lastShotFiredTimeMillis < (currentTimeMillis - maxFireRateMillis)) {
            if (alsoUpdateLastShotFiredTime) {
                lastShotFiredTimeMillis = currentTimeMillis;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This is a shortcut method that, by default, also updates
     * lastShotFiredTimeMillis. See the full method with the boolean parameter
     * for details. Use this mainly for the primary shooting mechanism only, and
     * use allowedToFire(false) otherwise (e.g.: the laser effect).
     */
    private boolean allowedToFire() {
        return allowedToFire(true);
    }

    /**
     * To make a Spatial have AI capabilities, simply pass it (and other stuff)
     * to one of the constructors of the AI classes (e.g.: MovingAI or
     * DodgingAI). Then, so that it's easier to keep track and enable keeping
     * the AIs "alive", put similar AIs together in a list (e.g.: movingAIs or
     * dodgingAIs).
     */
    private void initAI() {
        // TODO: Make some of the AI always move/patrol some area/radius
        dodgingAIs.put(golemGeom, new WalkingGroundDodgingAI(golemSpatial, golemGeom, rootNode, physicsSpace, cam));
        dodgingAIs.put(golemGeom2, new WalkingGroundDodgingAI(golemSpatial2, golemGeom2, rootNode, physicsSpace, cam));
        dodgingAIs.put(golemGeom3, new WalkingGroundDodgingAI(golemSpatial3, golemGeom3, rootNode, physicsSpace, cam));
        dodgingAIs.put(golemGeom4, new WalkingGroundDodgingAI(golemSpatial4, golemGeom4, rootNode, physicsSpace, cam));
        dodgingAIs.put(golemGeom5, new WalkingGroundDodgingAI(golemSpatial5, golemGeom5, rootNode, physicsSpace, cam));

        dodgingAIs.put(hoverTankGeom, new AerialDodgingAI(hoverTankSpatial, hoverTankGeom, rootNode, physicsSpace, cam));
        dodgingAIs.put(hoverTankGeom2, new AerialDodgingAI(hoverTankSpatial2, hoverTankGeom2, rootNode, physicsSpace, cam));
        dodgingAIs.put(hoverTankGeom3, new AerialDodgingAI(hoverTankSpatial3, hoverTankGeom3, rootNode, physicsSpace, cam));

//        dodgingAIs.put(ninjaGeom, new DodgingAI(ninjaSpatial,ninjaGeom,rootNode,physicsSpace,cam));


    }

    private void resetPlayerLocation() {
        player.setPhysicsLocation(DEFAULT_PLAYER_LOCATION);
        if (camInitDir == null) {
            camInitDir = cam.getDirection();
        }
        if (camInitUp == null) {
            camInitUp = cam.getUp();
        }
        cam.lookAtDirection(camInitDir, camInitUp);
    }

    private void showHitMarker() {
        rootNode.attachChild(hitMarker);
        hitMarkerLastShownTimeMillis = System.currentTimeMillis();
    }

    private void hideHitMarker() {
        rootNode.detachChild(hitMarker);
    }

    /**
     * Public static access modifiers used since it's like a Utility
     */
    public static Geometry getGeomFromSpatial(Spatial spatial) {
        return ((Geometry) ((Node) spatial).getChild(0));
    }

    private Geometry loadModelToGeom(String modelFilePath) {
        return getGeomFromSpatial(assetManager.loadModel(modelFilePath));
    }

    private void initSkyBox() {
        Texture west = assetManager.loadTexture("Textures/frontImage.png");
        Texture east = assetManager.loadTexture("Textures/backImage.png");
        Texture north = assetManager.loadTexture("Textures/leftImage.png");
        Texture south = assetManager.loadTexture("Textures/rightImage.png");
        Texture top = assetManager.loadTexture("Textures/upImage.png");
        Texture bot = assetManager.loadTexture("Textures/downImage.png");

        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, top, bot);
        rootNode.attachChild(sky);
    }

    private void updateHitMarker() {
        // Remove hit marker if it stays too long
        if (hitMarker.getParent() != null && (System.currentTimeMillis() > (hitMarkerLastShownTimeMillis + SHOW_HIT_MARKER_MAX_DURATION_MILLIS))) {
            hideHitMarker();
        }
    }

    // TODO: These kinect stuff should be moved to its own class, such as KinectControls, KinectEvents, etc.
    /**
     * This is a kinect event listener. This is run whenever kinect skeleton
     * reading becomes available, when the readings are previously NOT
     * available.
     */
    private void onKinectSkeletonActivated() {
        kinectSkeletonActive = true;

        kinectControlsStatusText.setText("Kinect Controls Active");
        showKinectControlsStatusText();

        // Decrease the difficulty since aiming with kinect is harder (than mouse)
        for (DodgingAI curDodgingAI : dodgingAIs.values()) {
            curDodgingAI.setDodgeMoveRateToSlowDefault(); // Slow down AI dodging
        }
    }

    public float getCurrentTimePerFrame() {
        return tpf;
    }

    /**
     * This is a kinect event listener. This is run whenever kinect skeleton
     * reading becomes NOT available, when the readings are previously
     * available.
     */
    private void onKinectSkeletonDeactivated() {
        kinectSkeletonActive = false;

//        kinectControlsStatusText.setText("Kinect Controls Inactive");
        hideKinectControlsStatusText();

        // Increase the difficulty since aiming with mouse is easier (than kinect)
        for (DodgingAI curDodgingAI : dodgingAIs.values()) {
            curDodgingAI.setDodgeMoveRateToFastDefault(); // Speed up AI dodging
        }
    }

    /**
     * @param targetText Alternate the visibilty of this text.
     * @param parentNodeOfTargetText The parent node of the text, which is used
     * to add/remove the text to achieve blinking effect.
     */
    private void toggleVisibility(BitmapText targetText, Node parentNodeOfTargetText) {
        if (targetText.getParent() != parentNodeOfTargetText) { // Make visible
            parentNodeOfTargetText.attachChild(targetText);
//                            textToBlink.setColor(new ColorRGBA(textToBlink.getRed(),textToBlink.getGreen(),textToBlink.getBlue(),1f));
        } else { // Make invisible
            parentNodeOfTargetText.detachChild(targetText);
//                            textToBlink.setColor(new ColorRGBA(textToBlink.getRed(),textToBlink.getGreen(),textToBlink.getBlue(),0f));
        }
    }

    private void makeCannonBall() {

        Geometry bombGeo = new Geometry("cannon ball", ballSphere);
        bombGeo.setMaterial(enemyBombMat);

        bombGeo.setLocalTranslation(hoverTankGeom.getLocalTranslation().x, hoverTankGeom.getLocalTranslation().y - 5, hoverTankGeom.getLocalTranslation().z);

        SphereCollisionShape bombCollisionShape = new SphereCollisionShape(1f);
        RigidBodyControl bombNode = new BombControl(assetManager, bombCollisionShape, 1);

        Vector3f playerPosition = new Vector3f(weapon.getLocalTranslation().x + 20, weapon.getLocalTranslation().y - 45, weapon.getWorldTranslation().z);

        bombNode.setLinearVelocity(playerPosition);

        bombGeo.addControl(bombNode);
        rootNode.attachChild(bombGeo);
        getPhysicsSpace().add(bombNode);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    public static float getMaxShootRange() {
        return maxShootRange;
    }

    private void updateCollisionResultsOfShootablesWithinPlayerAim() {
        // 1. Reset results list.
        collisionResultsOfShootablesWithinPlayerAim = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());

        // 3. Collect intersections between Ray and Shootables in results list.
        shootables.collideWith(ray, collisionResultsOfShootablesWithinPlayerAim);
    }

    private boolean aShootableIsWithinPlayerAim() {
        if (collisionResultsOfShootablesWithinPlayerAim == null) {
            return false;
        }

        return (collisionResultsOfShootablesWithinPlayerAim.size() > 0);
    }

    private CollisionResult getCollisionResultOfClosestShootableWithinPlayerAim() {
        if (collisionResultsOfShootablesWithinPlayerAim == null) {
            return null;
        }
        return collisionResultsOfShootablesWithinPlayerAim.getClosestCollision();
    }

    private void initAudio() {
        /* gun shot sound is to be triggered by a mouse click. */
        audio_gun = new AudioNode(assetManager, "/Sounds/laser.wav", false);
        audio_gun.setLooping(true);
        audio_gun.setVolume(0.5f);
        rootNode.attachChild(audio_gun);

        audio_explode = new AudioNode(assetManager, "/Sounds/explode.wav", false);
        audio_explode.setLooping(true);
        audio_explode.setVolume(0.5f);
        rootNode.attachChild(audio_explode);

        audio_win = new AudioNode(assetManager, "/Sounds/win.wav", false);
        audio_win.setLooping(true);
        audio_win.setVolume(0.5f);
        rootNode.attachChild(audio_win);

        audio_lose = new AudioNode(assetManager, "/Sounds/gameover.wav", false);
        audio_lose.setLooping(true);
        audio_lose.setVolume(1f);
        rootNode.attachChild(audio_lose);

        audio_impact = new AudioNode(assetManager, "/Sounds/impact.wav", false);
        audio_impact.setLooping(true);
        audio_impact.setVolume(0.1f);
        rootNode.attachChild(audio_impact);

        audio_background = new AudioNode(assetManager, "/Sounds/background.wav", false);
        audio_background.setLooping(true);  // activate continuous playing
        audio_background.setPositional(true);
        audio_background.setLocalTranslation(Vector3f.ZERO.clone());
        audio_background.setVolume(0.1f);
        rootNode.attachChild(audio_background);
        audio_background.play(); // play continuously!
    }

    private Material initMaterial(int terrainSize, float waterLevel, float normalizer) {
        // the material and its definitions can be found in:
        // jme3-libraries - jme3-terrain.jar
        // look at the j3md file to find the parameters
        Material mat = new Material(assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
        //mat.getAdditionalRenderState().setWireframe(true);
        Texture grass = assetManager.loadTexture("Textures/metal2.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        Texture dirt = assetManager.loadTexture("Textures/metal2.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
//        Texture rock = assetManager.loadTexture("Textures/DirtWater.jpg");
//        rock.setWrap(Texture.WrapMode.Repeat);
//        Texture dirtWater = assetManager.loadTexture("Textures/Test.jpg");
//        dirtWater.setWrap(Texture.WrapMode.Repeat);
//        mat.setTexture("region1ColorMap", dirtWater);
        mat.setTexture("region2ColorMap", grass);
        mat.setTexture("region3ColorMap", dirt);
//        mat.setTexture("region4ColorMap", rock);
        mat.setTexture("slopeColorMap", dirt);
        //
        float step = (normalizer - waterLevel);
//        mat.setVector3("region1", new Vector3f(0, waterLevel, 50f)); //startheight, endheight, scale
        mat.setVector3("region2", new Vector3f(waterLevel, waterLevel + step, 100f)); //startheight, endheight, scale
        mat.setVector3("region3", new Vector3f(waterLevel + step, waterLevel + 2 * step, 130f)); //startheight, endheight, scale
        mat.setVector3("region4", new Vector3f(waterLevel + 2 * step, normalizer, 160f)); //startheight, endheight, scale
        //
        mat.setFloat("terrainSize", terrainSize);
        mat.setFloat("slopeTileFactor", 32f);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        return (mat);
    }
}
