/**
 * TODO description
 */



public class Main {
	protected static IChat initGUI(IColor cs,IEncrypter e) {
		IChat gui = new ChatGUI(cs, e);
		return gui;
	}
}