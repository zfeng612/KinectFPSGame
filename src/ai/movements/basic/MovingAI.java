package ai.movements.basic;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
/*
TODO: USE STATE MACHINES FOR MOVING AND DOGING AI!
USE ENUM: public enum SMStates{MOVING_TO_TARGET,TARGET_REACHED}
STATE MOVING_TO_TARGET: KEEP MOVING TO TARGET UNTIL REACHED
JUST CHANGE TARGET LOCATION, which is checked in MOVING_TO_TARGET, when player moves prematurely
WHEN NEED TO STOP, CHANGE STATE TO TARGET_REACHED 
 */
/**
 *
 * @author Peter Purwanto
 * 
 * This class wraps the given Spatial/Geometry with default interfaces
 * for a moving AI's most common tasks, such as moving forward, backward,
 * left, right, up, and down.
 * Basic movement safety checks to avoid collisions, etc. are also implemented.
 * The specifics are implemented/overridden by sub-classes.
 */
public abstract class MovingAI{
    /**
     * List of possible move types (constants).
     * Mainly used to supply a move type to the general move() function.
     */
    public enum MoveType{LEFT,RIGHT,FORWARD,BACKWARD,UP,DOWN}
    
    /**
     * Allow being only up to this close in front of obstacles.
     * Mainly used for collision avoidance.
     * Unit: meters.
     */
    protected float minAllowedDistanceInFrontOfObstacle = 2;
    
    /**
     * Spatial = Mainly used for animations.
     */
    protected Spatial bodySpatial;
    /**
     * Geometry = Mainly used for collision detection.
     */
    protected Geometry bodyGeom;
    
//    protected Spatial sceneModel; @param sceneModel The main scene model/object. (Mainly used for AI collision and fall avoidance.)
    
    protected Node rootNode;
    
    protected PhysicsSpace physicsSpace;

    public Geometry getBodyGeom() {
        return bodyGeom;
    }
    
//    protected FPSGame mainGameInstance;
    
    protected RigidBodyControl bodyPhysics;
    
    /**
     * Maximum allowed distance between the AI's body and the ground before
     * treating the distance as a cliff's distance to avoid moving to. This
     * is mainly used to prevent the AI from falling off the map/ground.
     */
//    public static final float MAX_ALLOWED_CLIFF_DISTANCE = 10f;
    

    
    /**
     * @param bodySpatial The AI's Spatial body that is to be controlled. (Mainly used for animations.)
     * @param bodyGeom The AI's Geometry body that is to be controlled. (Mainly used for collision and fall avoidance.)
     * @param rootNode The root node where the bodySpatial is attached to. (Mainly used for collision and fall avoidance.)
     * @param physicsSpace The physics space where the body's physics will be placed into. (Mainly used to wrap the body with Kinematic RigidBodyControl.)
     */
    public MovingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace){
        this.bodySpatial = bodySpatial;
        
        // TODO: Always extract geometry from the given spatial, so 1 less param. to pass to constructor? But may not be safe if geom. doesn't have the expected kind of spatial.
//            try{ // Try to also get Geometry from the given Spatial
//                this.bodyGeom = FPSGame.getGeomFromSpatial(bodySpatial);
//            }
//            catch (Exception e){
//                this.bodyGeom = null;
//            }
        this.bodyGeom = bodyGeom;
        
//        this.sceneModel = sceneModel;
        
        this.rootNode = rootNode;
        
        this.physicsSpace = physicsSpace;
//        this.mainGameInstance = mainGameInstance;
    }
    
    /**
     * Internal use only. Outside classes should use getLocation(), the version
     * that returns cloned copies of this method's return values.
     */
    private Vector3f getLocationUncloned(){
        return bodyGeom.getLocalTranslation();
    }
    /**
     * A shortcut method to get AI Body's current location.
     * This also returns a clone copy of the vector, so manipulations to it
     * won't affect the actual location of the AI Body.
     */
    public Vector3f getLocation(){
        return getLocationUncloned().clone();
    }
    
    /**
     * Internal use only. Outside classes should use getRotation(), the version
     * that returns cloned copies of this method's return values.
     */
    private Quaternion getRotationUncloned(){
        return bodyGeom.getLocalRotation();
    }
    /**
     * A shortcut method to get AI Body's current rotation.
     * This also returns a clone copy of the Quaternion, so manipulations to it
     * won't affect the actual rotation of the AI Body.
     */
    public Quaternion getRotation(){
        return getRotationUncloned().clone();
    }
    
    public static Vector3f getDirectionFromRotation(Quaternion rotation){
        return rotation.getRotationColumn(2);
    }
    
    private Vector3f getDirectionUncloned(){
        return getDirectionFromRotation(getRotationUncloned());
    }
    /**
     * A shortcut method to get AI Body's current facing direction.
     * This also uses a clone copy from the AI Body's current rotation.
     * So, manipulations to the returned vector from the cloned rotation also
     * won't affect the actual facing direction of the AI Body.
     */
    public Vector3f getDirection(){
        return getDirectionUncloned().clone();
    }
    
    
    
    /**
     * A shortcut method for setLocalTranslation,
     * to instantly move (translate) the AI Body to target location.
     */
    private void translateTo(Vector3f targetLoc){
            if (!safeToMove(targetLoc)){
//                System.out.println("NOT SAFE TO MOVE!");
                return;
            }
        bodyGeom.setLocalTranslation(targetLoc);
    }
    /**
     * A shortcut method to make the AI Body to look at target location.
     * (Look at is fixed along Y-Axis for common use.)
     */
    public void lookAt(Vector3f targetLoc){
        bodyGeom.lookAt(targetLoc, Vector3f.UNIT_Y);
    }
    // TODO: Move methods should be used in state machines, and don't use amountInMeters. Instead use target location and smoothly move (with a pre-specified or calculated velocity) from current to target location.
    /**
     * General move function that moves the AI according to the specified move type.
     * This mainly allows for move commands with non-hardcoded calls to
     * functions of a specific move type.
     * @param moveType The type of movement. Must be one of the predefined MoveType.
     * @param amountInMeters How much to move with the specified type.
     */
    public void move(MoveType moveType,float amountInMeters){
        switch (moveType){
            case FORWARD:
                moveForward(amountInMeters);
                break;
            case BACKWARD:
                moveBackward(amountInMeters);
                break;
            case LEFT:
                moveLeft(amountInMeters);
                break;
            case RIGHT:
                moveRight(amountInMeters);
                break;
            case UP:
                flyUp(amountInMeters);
                break;
            case DOWN:
                flyDown(amountInMeters);
                break;
        }
    }
    /**
     * Move with Time Per Frame (TPF) taken into account, so that move amount/speed
     * is consistent across machines with different CPU speeds.
     */
    public void moveWithTPF(MoveType moveType,float amountInMeters,float tpf){
        move(moveType,amountInMeters * tpf);
    }
    
    public Vector3f getProjectedMoveVector(MoveType moveType,float amountInMeters){
        switch (moveType){
            case FORWARD:
                return getProjectedMoveForwardVector(amountInMeters);
            case BACKWARD:
                return getProjectedMoveBackwardVector(amountInMeters);
            case LEFT:
                return getProjectedMoveLeftVector(amountInMeters);
            case RIGHT:
                return getProjectedMoveRightVector(amountInMeters);
            case UP:
                return getProjectedFlyUpVector(amountInMeters);
            case DOWN:
                return getProjectedFlyDownVector(amountInMeters);
            default:
                return null;
        }
    }
    
    public Vector3f getProjectedMoveVectorWithTPF(MoveType moveType,float amountInMeters,float tpf){
        return getProjectedMoveVector(moveType,amountInMeters * tpf);
    }
    
    
    
    // TODO: These lookAt() are quick fixes for setting the correct look direction. In the future, also provide strafe movements that doesn't change the look directions. Even use a state machine for smooth, animated turning.
    public void moveForward(float amountInMeters){
        // TODO: Set bounds to move amount or just let any amount? E.g., for moveForward, if amount is < 0 and is not handled, the effect will actually do a moveBackward!
//            if (amountInMeters < 0f){
//                amountInMeters = 0f;
//            }
        
        Vector3f curLoc = getProjectedMoveForwardVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void moveForwardWithTPF(float amountInMeters,float tpf){
        moveForward(amountInMeters * tpf);
    }
    /**
     * Mainly used for predicting the location of where the AI would be at,
     * if the AI is to move forward by the given distance amount;
     * doesn't actually move the AI.
     */
    public Vector3f getProjectedMoveForwardVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.z += amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedMoveForwardVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedMoveForwardVector(amountInMeters * tpf);
    }
    
    
    
    public void moveBackward(float amountInMeters){        
        Vector3f curLoc = getProjectedMoveBackwardVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void moveBackwardWithTPF(float amountInMeters,float tpf){
        moveBackward(amountInMeters * tpf);
    }
    public Vector3f getProjectedMoveBackwardVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.z -= amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedMoveBackwardVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedMoveBackwardVector(amountInMeters * tpf);
    }
    
    
    
    public void moveLeft(float amountInMeters){
        Vector3f curLoc = getProjectedMoveLeftVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void moveLeftWithTPF(float amountInMeters,float tpf){
        moveLeft(amountInMeters * tpf);
    }
    public Vector3f getProjectedMoveLeftVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.x += amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedMoveLeftVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedMoveLeftVector(amountInMeters * tpf);
    }
    
    
    
    
    public void moveRight(float amountInMeters) {
        Vector3f curLoc = getProjectedMoveRightVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void moveRightWithTPF(float amountInMeters,float tpf){
        moveRight(amountInMeters * tpf);
    }
    public Vector3f getProjectedMoveRightVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.x -= amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedMoveRightVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedMoveRightVector(amountInMeters * tpf);
    }
    
    
    
    public void flyUp(float amountInMeters) {
        Vector3f curLoc = getProjectedFlyUpVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void flyUpWithTPF(float amountInMeters,float tpf){
        flyUp(amountInMeters * tpf);
    }
    public Vector3f getProjectedFlyUpVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.y += amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedFlyUpVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedFlyUpVector(amountInMeters * tpf);
    }
    
    
    
    public void flyDown(float amountInMeters) {
        Vector3f curLoc = getProjectedFlyDownVector(amountInMeters);
        lookAt(curLoc);
        translateTo(curLoc);
    }
    public void flyDownWithTPF(float amountInMeters,float tpf){
        flyDown(amountInMeters * tpf);
    }
    public Vector3f getProjectedFlyDownVector(float amountInMeters){
        Vector3f projectedLoc = getLocation();
        projectedLoc.y -= amountInMeters;
        return projectedLoc;
    }
    public Vector3f getProjectedFlyDownVectorWithTPF(float amountInMeters,float tpf){
        return getProjectedFlyDownVector(amountInMeters * tpf);
    }
    
    
    
    // TODO: Add movement contraints to prevent bumping/going through other objects. Even do recoveries if already bumped to another object, etc.
    /**
     * To avoid bumping into obstacles/walls.
     * @return true when there is an obstacle/wall near the front.
     * Otherwise returns false.
     */
    public boolean obstacleCloseInFront(Vector3f targetLoc,Vector3f straightFacingDirection,Node rootNode){
        straightFacingDirection = straightFacingDirection.clone(); // Use temporary clone for safe predicted moves. (Because Java passes a COPY of the ADDRESS, this won't affect the original object/address at all.)
        // [REMOVE .clone() to directly see how the AI would move,etc.]
        
        CollisionResults obstacleInFrontResults = new CollisionResults();
        // Aim the ray from AI's target location
        // and current direction.
        // (assuming AI's standing straight up/AI's direction is centered/horizontal along y-axis).
        // Collect intersections between ray and all nodes in results list.
        rootNode.collideWith(new Ray(targetLoc, straightFacingDirection), obstacleInFrontResults);

            if (obstacleInFrontResults.size() > 0) { // Hit something
                CollisionResult closestObstacleInFront = obstacleInFrontResults.getClosestCollision();
                float dist = closestObstacleInFront.getDistance();
//                System.out.println("dist to obstacle = "+dist);
                    if (dist < minAllowedDistanceInFrontOfObstacle){
                        return true;
                    }
            }
        
        return false;
    }
    protected boolean obstacleCloseInFront(Vector3f targetLoc){
        return obstacleCloseInFront(targetLoc,getDirectionUncloned(),rootNode);
    }
    
    public static float getDistanceToGround(Vector3f targetLoc,Quaternion straightFacingRotation,Node rootNode){
        straightFacingRotation = straightFacingRotation.clone(); // Use temporary clone for safe predicted moves. (Because Java passes a COPY of the ADDRESS, this won't affect the original object/address at all.)
        // [REMOVE .clone() to directly see how the AI would rotate,etc.]
        CollisionResults groundObstacleResults = new CollisionResults();
        // Aim the ray from AI's target location
        // and current direction--rotated 90 degrees along the x-axis, to
        // get the direction as if the AI is to look down at the ground below
        // its feet/body. [REMOVE .clone() in ...getLocalRotation().clone()... to directly see how the AI would rotate]
        // (assuming AI's standing straight up/AI's direction is centered/horizontal along y-axis).
        // Collect intersections between ray and all nodes in results list.
        rootNode.collideWith(new Ray(targetLoc, MovingAI.getDirectionFromRotation(straightFacingRotation.fromAngleAxis(90f/180f*FastMath.PI,Vector3f.UNIT_X))), groundObstacleResults);

            if (groundObstacleResults.size() > 0) { // Hit something
                CollisionResult closestGroundObstacleBelow = groundObstacleResults.getClosestCollision();
                return closestGroundObstacleBelow.getDistance();
            }
            else{ // Hit nothing
                return Float.POSITIVE_INFINITY;
            }
    }
    protected float getDistanceToGround(Vector3f targetLoc){
        return getDistanceToGround(targetLoc,getRotationUncloned(),rootNode);
    }

    // Manual additional technique for cliffBelow: check with minimum x,y,z axis values.
    /**
     * To avoid falling out of the map/ground.
     * @return true when given/projected move in front will be out of the map/ground.
     * Otherwise returns false.
     */
    public static boolean cliffBelow(Vector3f targetLoc,Quaternion straightFacingRotation,Node rootNode){
            if (getDistanceToGround(targetLoc,straightFacingRotation,rootNode) < Float.POSITIVE_INFINITY) { // Hit something
                // For each hit, we know distance, impact point, name of geometry.
//                CollisionResult closestGroundObstacleBelow = groundObstacleResults.getClosestCollision();
//                float dist = closestGroundObstacleBelow.getDistance();
//                if (dist < MAX_ALLOWED_CLIFF_DISTANCE){
//                    System.out.println("NO CLIFF IN FRONT (that's >= 10f deep)!");
                    return false;
//                }
//                else{
////                    System.out.println("YES, CLIFF IN FRONT that's >= 10f deep!");
//                    return true;
//                }
            }
            else{ // Hit nothing
//                System.out.println("YES, CLIFF IN FRONT that's INFINITELY deep!");
                return true;
            }
    }
    protected boolean cliffBelow(Vector3f targetLoc){
        return cliffBelow(targetLoc,getRotationUncloned(),rootNode);
    }
    
    /**
     * Safe to move the AI body to specified target location only if, FOR EXAMPLE,
     * at target location, there will be no obstacle in front of or cliff below
     * the AI body/feet.
     */
    public abstract boolean safeToMove(Vector3f targetLoc,Quaternion straightFacingRotation);
    
    public boolean safeToMove(Vector3f targetLoc){
        return safeToMove(targetLoc,getRotationUncloned());
    }
}
