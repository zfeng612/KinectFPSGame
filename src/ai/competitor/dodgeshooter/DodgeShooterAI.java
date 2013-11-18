package ai.competitor.dodgeshooter;

import ai.attack.shooting.ShootingAI;
import ai.defense.dodging.DodgingAI;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.Node;

/**
 *
 * @author Peter Purwanto
 * 
 * 
 */
public class DodgeShooterAI{
    private DodgingAI dodgingAIInstance;
    private ShootingAI shootingAIInstance;
    
    public DodgeShooterAI(DodgingAI dodgingAIInstance,AssetManager assetManager,Node rootNode,PhysicsSpace physicsSpace){
        this.dodgingAIInstance = dodgingAIInstance;
        // TODO: These gets() are quick fixes, but very bad modularity!
        this.shootingAIInstance = new ShootingAI(dodgingAIInstance.getPlayerCamera(),dodgingAIInstance.getBodyGeom(),assetManager, rootNode, physicsSpace);
    }
    
    public DodgingAI getDodgingAIInstance() {
        return dodgingAIInstance;
    }
    
    public void dodgeShootWhenAppropriate(float tpf){
        dodgingAIInstance.dodgeWhenAppropriate(tpf);
        shootingAIInstance.shootWhenAppropriate();
    }

}
