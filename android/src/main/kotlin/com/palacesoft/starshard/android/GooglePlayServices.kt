package com.palacesoft.starshard.android

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.games.PlayGames
import com.palacesoft.starshard.GameServices
import com.palacesoft.starshard.R

class GooglePlayServices(private val activity: Activity) : GameServices {

    private val leaderboardId: String by lazy {
        activity.getString(R.string.leaderboard_high_score)
    }

    override fun submitScore(score: Int) {
        val client = PlayGames.getLeaderboardsClient(activity)
        client.submitScore(leaderboardId, score.toLong())
    }

    override fun showLeaderboard() {
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.isAuthenticated.addOnCompleteListener { authTask ->
            val isAuthenticated = authTask.isSuccessful && authTask.result.isAuthenticated
            if (isAuthenticated) {
                openLeaderboard()
            } else {
                signInClient.signIn().addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        openLeaderboard()
                    } else {
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Sign in to Google Play to view leaderboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun openLeaderboard() {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent ->
                activity.startActivityForResult(intent, RC_LEADERBOARD)
            }
            .addOnFailureListener {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Could not open leaderboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    companion object {
        private const val RC_LEADERBOARD = 9004
    }
}
