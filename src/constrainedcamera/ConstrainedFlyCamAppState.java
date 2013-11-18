/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package constrainedcamera;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

/**
 *
 * @author Roochie
 */
public class ConstrainedFlyCamAppState extends AbstractAppState {

    private Application app;
    private ConstrainedFlyByCamera flyCam;

    public ConstrainedFlyCamAppState() {
    }    

    /**
     *  This is called by SimpleApplication during initialize().
     */
    void setCamera( ConstrainedFlyByCamera cam ) {
        this.flyCam = cam;
    }
    
    public ConstrainedFlyByCamera getCamera() {
        return flyCam;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        
        this.app = app;

        if (app.getInputManager() != null) {
        
            if (flyCam == null) {
                flyCam = new ConstrainedFlyByCamera(app.getCamera());
            }
            
            flyCam.registerWithInput(app.getInputManager());            
        }               
    }
            
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        flyCam.setEnabled(enabled);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();

        flyCam.unregisterInput();        
    }


}

