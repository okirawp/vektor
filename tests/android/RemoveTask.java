/** Emulator-only framework task removal. Never bundled with Rota. */
public class RemoveTask {
 public static void main(String[] args)throws Exception{
  Object service=Class.forName("android.app.ActivityTaskManager").getDeclaredMethod("getService").invoke(null);
  Class<?> api=Class.forName("android.app.IActivityTaskManager");
  Object removed=api.getMethod("removeTask",int.class).invoke(service,Integer.parseInt(args[0]));
  System.out.println("removed="+removed);
 }
}
