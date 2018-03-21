package ca.google.musicplayerexample;

import android.content.Context;
import android.widget.MediaController;

public class MusicController extends MediaController {

    // Constructor
    public MusicController(Context c){
        super(c);
    }

    // Overriding the hide() method so it doesn't automatically hide after 3 seconds
    public void hide(){}

}
