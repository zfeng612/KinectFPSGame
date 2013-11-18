package ai.defense.dodging;

import ai.core.ShootingPlayerMonitorer;
import ai.movements.basic.MovingAI;
import ai.movements.basic.MovingAI.MoveType;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// TODO: Future idea for this game: A survival game named Dodge Defender, where enemies try to walk through a goal and player's supposed to defend the goal and shoot down enemies before any of them get to the goal. And some enemies would dodge, etc.
// TODO: This needs more modularization--take out some functions to ShootingPlayerMonitorerForAI
/**
 *
 * @author Peter Purwanto
 * 
 * This class adds more functionality to MovingAI:
 * In addition for handling a moving AI body, this class deals with ways to help
 * the AI body decide when and how to dodge the player's aim/camera.
 * The underlying movement specifics will be implemented by subclasses, which,
 * in turn, supplies an instance of a specific MovingAI implementation.
 */
public abstract class DodgingAI extends ShootingPlayerMonitorer{

    /*--- SETTINGS ---*/
    /**
     * AI starts dodging whenever the player's location or aim is within this radius.
     */
    public static final float DEFAULT_AIM_DODGE_RADIUS_IN_METERS = 5;
    /**
     * AI force dodges/speed ups when player location is this close to AI's location.
     */
    public static final float DEFAULT_CLOSENESS_FORCE_DODGE_RADIUS_IN_METERS = 25;
    /**
     * NOTE: DEFAULT_SLOW_DODGE_MOVE_RATE_IN_METERS must NOT be 0 or less.
     */
    public static final float DEFAULT_SLOW_DODGE_MOVE_RATE_IN_METERS = .5f;
    /**
     * NOTE: DEFAULT_FAST_DODGE_MOVE_RATE_IN_METERS must NOT be 0 or less.
     */
    public static final float DEFAULT_FAST_DODGE_MOVE_RATE_IN_METERS = 3f;
    /**
     * NOTE: DEFAULT_DODGE_MOVE_RATE_IN_METERS must NOT be 0 or less.
     */
    public static final float DEFAULT_DODGE_MOVE_RATE_IN_METERS = DEFAULT_FAST_DODGE_MOVE_RATE_IN_METERS;
    /**
     * Multiplies dodgeMoveRateInMeters whenever forceDodge() takes effect
     * NOTE: DEFAULT_FORCED_DODGE_MOVE_RATE_MULTIPLIER must NOT be 0 or less.
     */
    public static final float DEFAULT_FORCED_DODGE_MOVE_RATE_MULTIPLIER = 3;
    
    /**
     * The move rate used in farthest possible projected dodge moves. This should be very large
     * to prevent "stuttering"/zig-zag dodging caused by super-"greedy" picking
     * when projections are very close/has slow move rate.
     * NOTE: DEFAULT_FARTHEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS must NOT be 0 or less.
     */
    public static final float DEFAULT_FARTHEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS = DEFAULT_FAST_DODGE_MOVE_RATE_IN_METERS * DEFAULT_FORCED_DODGE_MOVE_RATE_MULTIPLIER;
    /**
     * The move rate used in closest possible projected dodge moves.
     * Mainly used to check if there's a collision very close ahead.
     * So, this should be very small.
     * NOTE: DEFAULT_CLOSEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS must NOT be 0 or less.
     */
    public static final float DEFAULT_CLOSEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS = .001f;
    /**
     * The chance/probability percentage that the AI should (intentionally)
     * fail to dodge (not dodge at all), so that it's not so hard to shoot at.
     * Should be between 0 and 100.
     * For example: 0 means never fails to at least attempt to dodge.
     * 100 means to never even attempt dodging at all.
     */
    public static final int DEFAULT_DODGE_FAIL_PROBABILITY_PERCENTAGE = 10;
    /**
     * How long should AI keep failing to "notice" that it should dodge.
     * Only takes effect is dodgeFailProbabilityPercentage > 0.
     */
    public static final int DEFAULT_DODGE_FAIL_EFFECT_DURATION_MILLIS = 1000;
    /**
     * How long should AI avoid intentionally failing to dodge
     * and also how long to recover from failing to dodge,
     * right after DEFAULT_DODGE_FAIL_EFFECT_DURATION_MILLIS is completed.
     * Only takes effect is dodgeFailProbabilityPercentage > 0.
     * Depends on dodgeFailProbabilityPercentage.
     * For example, let dodgeFailProbabilityPercentage = 50%: getting random
     * probability of 50% (or less), dodging will fail
     * for this variable's duration. Otherwise,
     * if get random prob. of 51% (or more), then dodging won't fail
     * for this variable's duration.
     */
    public static final int DEFAULT_DODGE_FAIL_AVOIDANCE_RECOVERY_DURATION_MILLIS = 3000;
    /**
     * How long should an AI smoothly perform continous dodges. I.e., once
     * AI starts dodging in a direction, it won't check and pick other dodge
     * directions until the current one is done. This is to prevent stuttering/
     * zig-zag picks by the AI.
     */
    public static final long DEFAULT_SMOOTH_CONSECUTIVE_DODGES_DURATION_MILLIS = 5000;
    /**
     * How long should an AI force dodge if it's been hit/shot by the player.
     * (Unit: milliseconds.)
     */
    public static final long DEFAULT_HIT_AI_FORCED_DODGE_DURATION_MILLIS = 1000;
    /*--- END SETTINGS ---*/
    
    /**
     * Save a global copy of last received Time Per Frame (TPF) value.
     */
    private float tpf = 1;
 
    /**
     * AI starts dodging whenever the player's location or aim is within this radius.
     */
    private float aimDodgeRadius = DEFAULT_AIM_DODGE_RADIUS_IN_METERS;
    private float closenessForceDodgeRadius = DEFAULT_CLOSENESS_FORCE_DODGE_RADIUS_IN_METERS;
    /**
     * NOTE: dodgeMoveRateInMeters must NOT be 0 or less.
     */
    private float dodgeMoveRateInMeters = DEFAULT_DODGE_MOVE_RATE_IN_METERS;
    /**
     * NOTE: forcedDodgeMoveRateMultiplier must NOT be 0 or less.
     */
    private float forcedDodgeMoveRateMultiplier = DEFAULT_FORCED_DODGE_MOVE_RATE_MULTIPLIER;
    
    private int dodgeFailProbabilityPercentage = DEFAULT_DODGE_FAIL_PROBABILITY_PERCENTAGE;
    private long dodgeFailEffectEndTimeMillis = 0;
    private long dodgeFailAvoidanceRecoveryEndTimeMillis = 0;
    
    /** 
     * Use flag variable to make sure only able to try to dodge
     * again after done dodging previously
     */
    private boolean isStillForcedDodging = false;
    /**
     * Used to differentiate whether force dodge is smooth/not sped up.
     * Using this, non-smooth force dodges will override smooth ones.
     */
    private boolean forceDodgeIsSmooth = false;
    /**
     * Temporary buffer to hold the original dodgeMoveRateInMeters value
     * before multiplying it with forcedDodgeMoveRateMultiplier. This is used to
     * restore dodgeMoveRateInMeters to the old value, after
     * isStillForcedDodging is false again (or is done).
     * This is used so even if the dodgeMoveRateInMeters is no longer set
     * to the default initial value, it can still be restored.
     * Initialized to Not-a-Number to be able to be checked if it was
     * already set to a valid value before or not.
     * BUT BE CAREFUL! Never use NaN directly, as that will cause the AI
     * to "vanish". Always check using Float.isNaN(dodgeMoveRateInMetersBeforeForced).
     */
    private float dodgeMoveRateInMetersBeforeForced = Float.NaN;
    /**
     * Stores the time when smooth consecutive dodging should end.
     * May use DEFAULT_SMOOTH_CONSECUTIVE_DODGES_DURATION_MILLIS as default duration.
     */
    private long consecutiveDodgingEndTimeMillis = 0;
    /**
     * Used in conjunction with isStillForcedDodging, to store the time
     * when forced dodging should end. May also use
     * DEFAULT_HIT_AI_FORCED_DODGE_DURATION_MILLIS as default duration.
     */
    private long forcedDodgingEndTimeMillis = 0;
    
    /**
     * The underlying movement mechanism to use.
     */
    protected MovingAI movingAIInstance = null;
     
    /**
     * @param playerCamera The player's camera, whose aim is what the AI should dodge.
     */
    public DodgingAI(Camera playerCamera){
        super(playerCamera);
    }
    
    public Geometry getBodyGeom(){
        return movingAIInstance.getBodyGeom();
    }
    
    public void setDodgeMoveRateToSlowDefault() {
        this.dodgeMoveRateInMeters = DEFAULT_SLOW_DODGE_MOVE_RATE_IN_METERS;
    }
    
    public void setDodgeMoveRateToFastDefault() {
        this.dodgeMoveRateInMeters = DEFAULT_FAST_DODGE_MOVE_RATE_IN_METERS;
    }

    private void setDodgeFailProbabilityPercentage(int dodgeFailProbabilityPercentage) throws Exception {
            if (dodgeFailProbabilityPercentage < 0 || dodgeFailProbabilityPercentage > 100){
                System.err.println("dodgeFailProbabilityPercentage must be between 0 and 100 (inclusive). You gave "+dodgeFailProbabilityPercentage+"; it's ignored.");
                return;
            }
        this.dodgeFailProbabilityPercentage = dodgeFailProbabilityPercentage;
    }
    
    
    
    /**
     * Specific case that uses the AI bodyGeom's location,
     * to be passed to the general getDistanceToPlayerAim(srcPointLoc) method.
     */
    private float getDistanceToPlayerAim(){
        return getDistanceToPlayerAim(movingAIInstance.getLocation());
    }
    
    private float getPlayerAimDistanceToDodgeRadius(){
        return Math.abs(getDistanceToPlayerAim() - aimDodgeRadius);
    }
    
    private boolean playerAimWithinDodgeRadius(){
        return (getDistanceToPlayerAim() < aimDodgeRadius);
    }
    


    /**
     * Specific case that uses the AI bodyGeom's location,
     * to be passed to the general getDistanceToPlayerLocation(srcPointLoc) method.
     */
    private float getDistanceToPlayerLocation(){
        return getDistanceToPlayerLocation(movingAIInstance.getLocation());
    }
    
    private float getPlayerLocationDistanceToDodgeRadius(){
        return Math.abs(getDistanceToPlayerLocation() - closenessForceDodgeRadius);
    }
    
    private boolean playerLocationWithinDodgeRadius() {
        return (getDistanceToPlayerLocation() < closenessForceDodgeRadius);
    }
    
    private boolean playerWithinDodgeRadius(){
        return (playerAimWithinDodgeRadius() || playerLocationWithinDodgeRadius());
    }
    
    /**
     * @param srcPointLoc Gets the closest distance from this given point location
     * to either the player's location or projected aim--whichever is closest.
     * Then, subtract from it the dodge radius to get the distance to
     * the dodge radius around where the AI would start dodging.
     * Mainly used to compare which projected dodging actions would result
     * in the AI being further from the player's camera/aim.
     */
//    public float getDistanceToDodgeRadius(Vector3f srcPointLoc){
//        float locDis = getPlayerLocationDistanceToDodgeRadius(srcPointLoc),
//              aimDis = getPlayerAimDistanceToDodgeRadius(srcPointLoc);
//        
//                if (aimDis < locDis){
//                    return Math.abs(aimDis - aimDodgeRadius);
//                }
//                else{
//                    return Math.abs(locDis - DEFAULT_CLOSENESS_FORCE_DODGE_RADIUS_IN_METERS);
//                }
//    }
    /**
     * Specific case that uses the AI bodyGeom's location,
     * to be passed to the general getDistanceToDodgeRadius(srcPointLoc) method.
//     */
//    public float getDistanceToDodgeRadius(){
//        return getDistanceToDodgeRadius(movingAIInstance.getLocation());
//    }

    /**
     * @param srcPointLoc Checks whether the given location is close
     * enough/within the dodge radius.
     * If so, then making a dodge action is appropriate (returns true).
     * By using the forceDodge() methods, this can be bypassed
     * to always make dodging appropriate.
     */
    private boolean dodgeIsAppropriate(Vector3f srcPointLoc){
        // TODO: AI shouldn't dodge when player is aiming at the AI body behind walls or too far.
        // TODO: If want to be more precise when determining if close to dodge radius, check the distances from the head, feet, and side hands/hip of the bodyGeom to the camera. (I.e., check against all sides)
            if (isStillForcedDodging){
                if (System.currentTimeMillis() > forcedDodgingEndTimeMillis){
                    setDodgeMoveRateToBeforeForcedRate();
                    isStillForcedDodging = false;
                }
                else{
                    return true; // Bypass to always say dodging appropriate while forced dodging is active
                }
            }
            
            if (playerLocationWithinDodgeRadius()){
                return true; // Bypass to always dodge
            }
            
            // TODO: Only dodge if AI can "see" the player; i.e., 0-180 degrees ray shot (with AI's look direction as the 90th degree) to player's location is not blocked by other stuff
            if (!withinPlayerShootingRange()){
                return false; // Bypass to always not dodge if player's shot is too far to hit, anyway.
            }
        
        boolean appropriateResult = playerWithinDodgeRadius();
        
        long currentTimeMillis = System.currentTimeMillis();
        
        /*--- BEGIN INTENTIONAL DODGE FAIL/RECOVERY/AVOIDANCE ---*/
            if (dodgeFailProbabilityPercentage > 0){
                if (currentTimeMillis < dodgeFailEffectEndTimeMillis){ // Intentionally fail to dodge
                    appropriateResult = false;
                }

                if (currentTimeMillis > dodgeFailAvoidanceRecoveryEndTimeMillis){
                    Random randGen = new Random(System.nanoTime());
                    int randNum = randGen.nextInt(100); // Randomly gives an integer between 0 and 100 (inclusive)
                        if (randNum <= dodgeFailProbabilityPercentage){ // Intentionally fail to dodge
                            dodgeFailEffectEndTimeMillis = currentTimeMillis + DEFAULT_DODGE_FAIL_EFFECT_DURATION_MILLIS;
                            dodgeFailAvoidanceRecoveryEndTimeMillis = dodgeFailEffectEndTimeMillis + DEFAULT_DODGE_FAIL_AVOIDANCE_RECOVERY_DURATION_MILLIS;
                            appropriateResult = false;
                        }
                        else{ // Intentionally AVOID failing to dodge
                            dodgeFailAvoidanceRecoveryEndTimeMillis = currentTimeMillis + DEFAULT_DODGE_FAIL_AVOIDANCE_RECOVERY_DURATION_MILLIS;
                        }
                }
            }
        /*--- END INTENTIONAL DODGE FAIL/RECOVERY/AVOIDANCE ---*/
            
        return appropriateResult;
    }
    /**
     * Specific case that uses the AI bodyGeom's location,
     * to be passed to the general dodgeIsAppropriate(srcPointLoc) method.
     */
    protected boolean dodgeIsAppropriate(){
        return dodgeIsAppropriate(movingAIInstance.getLocation());
    }
    
    private Map<Vector3f,MoveType> projMovesVectors = new HashMap<Vector3f,MoveType>(MoveType.values().length);
    /**
     * This picks the best kind of move-away action and location for the AI:
     * project the distance to player aim and location when the AI goes away to
     * all possible projected points/directions,
     * and check whether being at any of the projected point/direction
     * will make the AI farther from player aim and location, or not.
     * Pick the action and location that gets the AI farthest from the player.
     * Also add some dodge failure probability and modifiable move rate
     * so it's not too hard/impossible for the player to ever hit the AI.
     * @return true if dodge was successfully carried out; false otherwise.
     */
    protected boolean dodgeToBestLocation(){
        updateProjMovesVectors();

        Set<Vector3f> projMovesKeys = projMovesVectors.keySet();
            if (projMovesKeys == null){
                return false;
            }
        Iterator<Vector3f> projMovesIterator = projMovesKeys.iterator();
            if (projMovesIterator == null){
                return false;
            }
            
            if (!projMovesIterator.hasNext()){
                return false;
            }
        
        Vector3f maxProjMoveEntry = projMovesIterator.next(), // Get first element for use as assumed max at start
                 curProjMoveEntry; // Buffer for use in the loop
        float maxProjectedDistanceToPlayerAim = getDistanceToPlayerAim(maxProjMoveEntry), // Distance to first element is assumed max at start
              curProjectedDistanceToPlayerAim; // Buffer for use in the loop
        float maxProjectedDistanceToPlayerLocation = getDistanceToPlayerLocation(maxProjMoveEntry), // Distance to first element is assumed max at start
              curProjectedDistanceToPlayerLocation; // Buffer for use in the loop
        
              while (projMovesIterator.hasNext()){
                  curProjMoveEntry = projMovesIterator.next();
                  curProjectedDistanceToPlayerAim = getDistanceToPlayerAim(curProjMoveEntry);
                  curProjectedDistanceToPlayerLocation = getDistanceToPlayerLocation(curProjMoveEntry);
                  
                    // Pick the dodge move type that gets the AI farthest from BOTH player's AIM and player's LOCATION.
                    // TODO: Move methods that can move the AI diagonally is needed, to avoid AI making stuttering/zig-zag moves and for more advanced dodging that checks distance to player location.
                    if (curProjectedDistanceToPlayerAim > maxProjectedDistanceToPlayerAim
                        && curProjectedDistanceToPlayerLocation > maxProjectedDistanceToPlayerLocation
                       ){
                        maxProjectedDistanceToPlayerAim = curProjectedDistanceToPlayerAim;
                        maxProjectedDistanceToPlayerLocation = curProjectedDistanceToPlayerLocation;
                        maxProjMoveEntry = curProjMoveEntry;
                    }
              }
        /*--- END PICKING BEST MOVE-AWAY/DODGE LOCATION ---*/
              
        movingAIInstance.moveWithTPF(projMovesVectors.get(maxProjMoveEntry), dodgeMoveRateInMeters,tpf);
        return true;
    }
    
    private void updateProjMovesVectors() {
        // TODO: Move rate should vary depending on the distance AI is from player.
        // E.g., when too close, move away quickly; when far/maxing the dodge radius,
        // move away way slower.
        
            if (!projMovesVectors.isEmpty()){ // Already got projections previously
                    // To avoid stuttering/zig-zag moves:
                    if (isStillForcedDodging){
                        return; // Don't update
                    }

                    // Also to avoid stuttering/zig-zag moves: If already been dodging continously during the set duration, don't update.
                long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis > consecutiveDodgingEndTimeMillis){
                        consecutiveDodgingEndTimeMillis = currentTimeMillis + DEFAULT_SMOOTH_CONSECUTIVE_DODGES_DURATION_MILLIS;
                    }
                    else{
                        return; // Don't update
                    }
            }
            
        /*--- BEGIN PICKING BEST MOVE-AWAY/DODGE LOCATION ---*/
        // Vector3f is the map key here, instead of MoveType, mainly because it's the most accessed and the MoveType is accessed only once at the end.
        projMovesVectors.clear();
            for (MoveType curMoveType : MoveType.values()){
                Vector3f curProjMoveVector = movingAIInstance.getProjectedMoveVector(curMoveType,DEFAULT_FARTHEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS),
                         curClosestProjMoveVector = movingAIInstance.getProjectedMoveVector(curMoveType,DEFAULT_CLOSEST_PROJECTED_DODGE_MOVE_RATE_IN_METERS);
                
                    if (curProjMoveVector != null){
                        movingAIInstance.lookAt(curProjMoveVector); // Current view direction is used by safeToMove, so set it to the projected direction! This will visually look as if the AI is really inspecting the available moves.
                            if(movingAIInstance.safeToMove(curProjMoveVector) && movingAIInstance.safeToMove(curClosestProjMoveVector)){
                                projMovesVectors.put(curProjMoveVector,curMoveType);
                            }
                    }
            }
//        projMovesVectors.put(getProjectedMoveLeftVector(dodgeMoveRateInMeters),MoveType.LEFT);
//        projMovesVectors.put(getProjectedMoveRightVector(dodgeMoveRateInMeters),MoveType.RIGHT);
//        projMovesVectors.put(getProjectedMoveForwardVector(dodgeMoveRateInMeters),MoveType.FORWARD);
//        projMovesVectors.put(getProjectedMoveBackwardVector(dodgeMoveRateInMeters),MoveType.BACKWARD);
    }

    /**
     * Forced dodging for a specified duration.
     * (E.g., forced as in don't care if player's aim is close or not;
     * just keep dodging for the specified duration!)
     * @param applySpeedBoost whether to boost the dodging speed.
     * Only force dodge if not currently already,
     * OR: current force dodging is a smooth one (with no boost) and
     * now speed boost is to be applied (which is to become no longer smooth).
     */
    private void forceDodge(long durationMillis,boolean applySpeedBoost){
        if (!isStillForcedDodging || (forceDodgeIsSmooth && applySpeedBoost)){
                
            isStillForcedDodging = true;
            
                if (applySpeedBoost){
                    forceDodgeIsSmooth = false;
                }
                else{
                    forceDodgeIsSmooth = true;
                }
                
            forcedDodgingEndTimeMillis = System.currentTimeMillis() + durationMillis;
            
                if (applySpeedBoost){
                    setDodgeMoveRateToForcedRate();
                }
        }
    }
    
    /**
     * Force dodge to best location using default duration and applying speed boost.
     */
    public void forceDodge(){
        forceDodge(DEFAULT_HIT_AI_FORCED_DODGE_DURATION_MILLIS,true);
    }
    
    /**
     * Force dodge to best location using specified duration but NOT applying speed boost.
     */
    private void smoothForceDodge(long durationMillis){
        forceDodge(durationMillis,false);
    }
    
    /**
     * Force dodge to best location using default duration but NOT applying speed boost.
     */
    private void smoothForceDodge(){
        smoothForceDodge(DEFAULT_HIT_AI_FORCED_DODGE_DURATION_MILLIS);
    }
    
    /**
     * *** THIS IS THE MAIN LOGIC & ACTION PERFORMED BY THE DODGING AI
     * AND SHOULD BE CALLED/POLLED CONTINUOUSLY IN simpleUpdate()
     */
    public void dodgeWhenAppropriate(float tpf){
        this.tpf = tpf;
//        getDistanceToPlayerAim(); // For live debugging
        
        // TODO: Enemy should only dodge when they're within player's firing range.
        // TODO: Of all enemies within player's firing range that is NOT dodging/being shot by the player, randomly make 1 of the enemy--at a time--slowly move their aim at the player. Set a minimum locked-in aim time (or slow fire rate) for the AI, so as to give enough time for the player itself to get a chance to dodge/get out of range/even shoot it back.
        // TODO: SOUND FOR "ATTACK-IMMINENT": When an enemy starts aiming at the player, play an alarm sound that warns the player. Then, when AI's shot is very imminent, play alarm that's even more louder or have more frequent beeps.
        // TODO: COLORING FOR "ATTACK-IMMINENT" OR "ATTACKER": Make the body of the enemy that's about to shoot/is shooting as red--or even blinking red, that is, for example, smoothly transitioning from original texture, being applied 0% opacity/100% transparency (invisible) red-colored overlay, having the overlay smoothly change to 50% opacity/50% transparency (partially-visible), and smoothly go back to having no overlay or have the overlay invisible again.
        // TODO: HUD: For better enemy tracking for the player, put red arrows on the corners of the screen that points to every out-of-screen enemies (or those behind walls) and the arrows' direction/position on the corners "follows" the corresponding enemy. Especially for stationary enemies, put red arrow above each of them, and even animate the arrows to "bounce" above the enemies.
        // TODO: For far future, even more advanced AI--one at a time--should even chase down the player!
        
        if (playerLocationWithinDodgeRadius()){
            forceDodge();
        }
        
        if (dodgeIsAppropriate()){
//            System.out.println("AI Distance To Player Location = "+getDistanceToPlayerLocation()); // For live debugging
//            smoothForceDodge();
            dodgeToBestLocation();
        }
        else{
            projMovesVectors.clear();
            lookAtPlayer();
        }
    }
    
    // TODO: Maybe AI should face the player after it finishes dodging and any associated animations? Even all the time when it's not moving and there are no obstacles blocking the AI's view of player? So that it makes sense how the AI is able to always see and dodge the player aim.
    private void lookAtPlayer() {
        movingAIInstance.lookAt(getPlayerLocation());
    }
    
    

    private boolean withinPlayerShootingRange() {
        return withinPlayerShootingRange(getDistanceToPlayerLocation());
    }
    

    
//    /**
//     * Invoked AFTER Every Animation End
//     * @param finishedAnimationControl
//     * @param finishedAnimationChannel
//     * @param finishedAnimationName 
//     */
//    @Override
//    public void onAnimCycleDone(AnimControl finishedAnimationControl, AnimChannel finishedAnimationChannel, String finishedAnimationName) {
//        super.onAnimCycleDone(finishedAnimationControl, finishedAnimationChannel, finishedAnimationName);
//        /* Add additional custom handling not already in superclass: */
//    }
//    
//    /**
//     * Invoked BEFORE Every Animation Start
//     * @param startingAnimationControl
//     * @param startingAnimationChannel
//     * @param startingAnimationName 
//     */
//    @Override
//    public void onAnimChange(AnimControl startingAnimationControl, AnimChannel startingAnimationChannel, String startingAnimationName) {
//        super.onAnimChange(startingAnimationControl, startingAnimationChannel, startingAnimationName);
//        /* Add additional custom handling not already in superclass: */
//        
//    }


    
    /**
     * Only applied if dodgeMoveRate NOT already applied a forced rate before.
     */
    protected void setDodgeMoveRateToForcedRate() {
        if (Float.isNaN(dodgeMoveRateInMetersBeforeForced)){
            dodgeMoveRateInMetersBeforeForced = dodgeMoveRateInMeters;
            dodgeMoveRateInMeters *= forcedDodgeMoveRateMultiplier;
        }
    }

    /**
     * Only applied if dodgeMoveRate IS already applied a forced rate before.
     */
    protected void setDodgeMoveRateToBeforeForcedRate() {
        if (!Float.isNaN(dodgeMoveRateInMetersBeforeForced)){
            dodgeMoveRateInMeters = dodgeMoveRateInMetersBeforeForced;
            dodgeMoveRateInMetersBeforeForced = Float.NaN;
        }
    }
}
