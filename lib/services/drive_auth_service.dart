import 'package:google_sign_in/google_sign_in.dart';
import 'package:shared_preferences/shared_preferences.dart';

class DriveAuthService {
  static final _googleSignIn = GoogleSignIn(
    scopes: ['https://www.googleapis.com/auth/drive.file'],
  );

  static final DriveAuthService _instance = DriveAuthService._();
  factory DriveAuthService() => _instance;
  DriveAuthService._();

  GoogleSignInAccount? get currentAccount => _googleSignIn.currentUser;

  Future<GoogleSignInAccount?> signIn() async {
    try {
      final account = await _googleSignIn.signIn();
      if (account != null) {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('drive_email', account.email);
      }
      return account;
    } catch (_) {
      return null;
    }
  }

  Future<bool> signInSilently() async {
    try {
      final account = await _googleSignIn.signInSilently();
      return account != null;
    } catch (_) {
      return false;
    }
  }

  Future<void> signOut() async {
    await _googleSignIn.disconnect();
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('drive_email');
  }

  Future<bool> isSignedIn() async => _googleSignIn.isSignedIn();

  Future<String?> getEmail() async {
    if (await isSignedIn()) return _googleSignIn.currentUser?.email;
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('drive_email');
  }
}
