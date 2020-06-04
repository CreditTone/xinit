package gz.xinit;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

public class XInitActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {
	
	private Activity mFocusActivity;
	
	private Set<Activity> activities = new HashSet<>();

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		activities.add(activity);
	}

	@Override
	public void onActivityStarted(Activity activity) {
		
	}

	@Override
	public void onActivityResumed(Activity activity) {
		mFocusActivity = activity;
	}

	@Override
	public void onActivityPaused(Activity activity) {
		mFocusActivity = null;
	}

	@Override
	public void onActivityStopped(Activity activity) {
		mFocusActivity = null;
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		
	}

	@Override
	public void onActivityDestroyed(Activity activity) {
		if (activities.contains(activity)) {
			activities.remove(activity);
		}
	}

	public Activity getFocusActivity() {
		return mFocusActivity;
	}

	public Set<Activity> getActivities() {
		return activities;
	}
	
	

}
