package earth.levi.dotenv;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;

// It's important that projects that have Java code in it will also be able to work with the plugin. 
class JavaActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.value_textview)).setText(Env.javaFoo);
    }
}
