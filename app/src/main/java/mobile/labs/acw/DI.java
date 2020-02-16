package mobile.labs.acw;

import android.content.Context;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import mobile.labs.acw.Data.ApiDataProvider;
import mobile.labs.acw.Data.DataRepository;
import mobile.labs.acw.Data.DataRepositoryImpl;
import mobile.labs.acw.Data.LocalDataProvider;
import mobile.labs.acw.Data.ScoreDataProvider;
import mobile.labs.acw.Data.ScoreDataProviderImpl;
import mobile.labs.acw.Data.ScoreDatabaseHelper;

/**
 * Created by ryan on 23/02/2018.
 */

public class DI {
    public static DataRepository provideDataRepository(Context context){
        return DataRepositoryImpl.getInstance(LocalDataProvider.getInstance(context), ApiDataProvider.getInstance());
    }

    public static ScoreDataProvider provideScoreProvider(Context context){
        return new ScoreDataProviderImpl(new ScoreDatabaseHelper(context));
    }
}
