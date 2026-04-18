import com.google.firebase.crashlytics.FirebaseCrashlytics

fun test() {
    val crashlytics = FirebaseCrashlytics.getInstance()
    crashlytics.sendUnsentReports()
}
