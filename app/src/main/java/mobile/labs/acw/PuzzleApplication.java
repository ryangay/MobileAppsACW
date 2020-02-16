package mobile.labs.acw;

import android.app.Application;

import mobile.labs.acw.Data.ApiDataProvider;

/**
 * Created by ryan on 23/02/2018.
 */

public class PuzzleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //DI.registerDependency(new ApiDataProvider());
    }
}
