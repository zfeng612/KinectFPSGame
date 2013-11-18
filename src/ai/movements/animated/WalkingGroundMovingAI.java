package ai.movements.animated;

import ai.movements.basic.GroundMovingAI;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
// TODO: Keep in mind that current animated AI design is mostly for ground units. For more types of animations, need to have AbstractAnimatedMovingGroundAI (extends AbstractAnimatedMovingAI--which extends MovingAI) with AnimatedWalkingAI as an implementation and AnimatedDrivingAI as another (e.g., for tanks and cars). And also even have AbstractAnimatedMovingAerialAI (extends AbstractAnimatedMovingAI--which extends MovingAI) with AnimatedFlyingAI as an implementation. Also consider creating and implementing interfaces instead of abstract classes.
/**
 *
 * @author Peter Purwanto
 * 
 * This class provides Walking animation interfaces for the given
 * ground-bound Spatial.
 * When instantiated, if the provided Spatial doesn't yet have an animation
 * control, one, along with a channel, will be created for it. The animation
 * listener implemented here is also added to the animation control.
 */
public class WalkingGroundMovingAI extends GroundMovingAI implements AnimEventListener{
    
    /**
     * List of possible animation names (constants).
     * TODO: Put all of the animation name constants in an AnimationName enum?
     */
    public static final String WALK_ANIMATION_NAME = "Walk";
    public static final String STAND_ANIMATION_NAME = "stand";
    
    public static final float DEFAULT_WALK_ANIMATION_SPEED = 1;
    public static final float FAST_WALK_ANIMATION_SPEED = DEFAULT_WALK_ANIMATION_SPEED * 2;

//    public static final int DEFAULT_UNINTERRUPTED_WALK_ANIMATION_DURATION_MILLIS = 100;
    
    protected AnimControl animationControl = null;
    protected AnimChannel animationChannel = null;

    public WalkingGroundMovingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace){
        super(bodySpatial, bodyGeom, rootNode, physicsSpace);
        
        this.animationControl = bodySpatial.getControl(AnimControl.class);
            if (animationControl != null){ // If can't get anim. control/is null, animation is effectively disabled.
                this.animationChannel = animationControl.createChannel();
                animationControl.addListener(this);
                setWalkAnimationSpeedToDefault(animationChannel);
            }
    }
    
    

    @Override
    public void moveForward(float amountInMeters) {
        doWalkAnimation();
        super.moveForward(amountInMeters);
    }
    @Override
    public void moveBackward(float amountInMeters) {
        doWalkAnimation();
        super.moveBackward(amountInMeters);
    }
    @Override
    public void moveLeft(float amountInMeters) {
        doWalkAnimation();
        super.moveLeft(amountInMeters);
    }
    @Override
    public void moveRight(float amountInMeters) {
        doWalkAnimation();
        super.moveRight(amountInMeters);
    }
    
    /**
     * Perform safe & graceful animation. I.e., this will perform the animation
     * ONLY IF the given animation channel and name is valid (not null)
     * AND currently playing animation (if any) is not the same as the new given
     * that's about to be started.
     * However, changing settings while the same animation is playing is allowed.
     * @param blendTime The time over which to blend the new animation
     * with the old/previous one. If zero, then no blending will occur and the new
     * animation will be applied instantly.
     */
    public static void animateGracefully(AnimChannel animationChannel,String animationName,float blendTime,boolean loopTheAnimation) {
        if (animationChannel != null && animationName != null){
//            animationChannel.setSpeed(FAST_WALK_ANIMATION_SPEED);
            animationChannel.setLoopMode(loopTheAnimation?LoopMode.Loop:LoopMode.DontLoop);
            
            boolean newAnimationNameIsSameAsCurrent = animationName.equals(animationChannel.getAnimationName());
                if (!newAnimationNameIsSameAsCurrent){
                    animationChannel.setAnim(animationName, blendTime);
                }
        }
    }

    public static void doWalkAnimation(AnimChannel animationChannel){
        animateGracefully(animationChannel,WALK_ANIMATION_NAME,0.25f,true);
    }
    public void doWalkAnimation(){
        doWalkAnimation(animationChannel);
    }
    
    public static void setWalkAnimationSpeedToDefault(AnimChannel animationChannel){
        animationChannel.setSpeed(DEFAULT_WALK_ANIMATION_SPEED);
    }
    public void setWalkAnimationSpeedToDefault(){
        setWalkAnimationSpeedToDefault(animationChannel);
    }
    
    public static void setWalkAnimationSpeedToFast(AnimChannel animationChannel){
        animationChannel.setSpeed(FAST_WALK_ANIMATION_SPEED);
    }
    public void setWalkAnimationSpeedToFast(){
        setWalkAnimationSpeedToFast(animationChannel);
    }
    
    public static void doStandAnimation(AnimChannel animationChannel){
        animateGracefully(animationChannel,STAND_ANIMATION_NAME,0.25f,false);
    }
    public void doStandAnimation(){
        doStandAnimation(animationChannel);
    }
    
    /**
     * Invoked AFTER Every Animation End
     * @param finishedAnimationControl
     * @param finishedAnimationChannel
     * @param finishedAnimationName 
     */
    public void onAnimCycleDone(AnimControl finishedAnimationControl, AnimChannel finishedAnimationChannel, String finishedAnimationName) {
        
    }

    /**
     * Invoked BEFORE Every Animation Start
     * @param startingAnimationControl
     * @param startingAnimationChannel
     * @param startingAnimationName 
     */
    public void onAnimChange(AnimControl startingAnimationControl, AnimChannel startingAnimationChannel, String startingAnimationName) {

    }
}
