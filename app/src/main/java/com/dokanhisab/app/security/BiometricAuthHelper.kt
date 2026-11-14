package com.dokanhisab.app.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability {
    READY,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

object BiometricAuthHelper {

    /**
     * Resolves the allowed BiometricPrompt / BiometricManager authenticators safely per API level.
     * On API 30+, STRONG + WEAK + DEVICE_CREDENTIAL is supported together.
     * On API 24-29, androidx.biometric:1.1.0 prohibits combining BIOMETRIC_STRONG with DEVICE_CREDENTIAL,
     * so we use BIOMETRIC_WEAK or DEVICE_CREDENTIAL.
     */
    fun resolveAllowedAuthenticators(sdkInt: Int = Build.VERSION.SDK_INT): Int {
        return if (sdkInt >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }

    fun checkCapability(context: Context): BiometricCapability {
        val manager = BiometricManager.from(context)
        val primaryResult = runCatching {
            manager.canAuthenticate(resolveAllowedAuthenticators())
        }.getOrElse {
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        }

        return when (primaryResult) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricCapability.UNAVAILABLE
            else -> BiometricCapability.UNAVAILABLE
        }
    }

    fun buildEnrollIntent(sdkInt: Int = Build.VERSION.SDK_INT): Intent {
        return if (sdkInt >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    resolveAllowedAuthenticators(sdkInt)
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        cancelText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val authenticators = resolveAllowedAuthenticators()
        val promptInfo = try {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(authenticators)
                .build()
        } catch (_: IllegalArgumentException) {
            // Defensive fallback to biometric-only prompt with explicit cancel button
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText(cancelText)
                .build()
        }

        prompt.authenticate(promptInfo)
    }
}
