package com.example.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.UserRole
import com.example.ui.onboarding.screens.AddPrescriptionOptionScreen
import com.example.ui.onboarding.screens.AuthScreen
import com.example.ui.onboarding.screens.CaregiverProfileScreen
import com.example.ui.onboarding.screens.MainAppPlaceholderScreen
import com.example.ui.onboarding.screens.ManualMedicineEntryScreen
import com.example.ui.onboarding.screens.OnboardingCameraScanScreen
import com.example.ui.onboarding.screens.OnboardingFinishScreen
import com.example.ui.onboarding.screens.PatientProfileScreen
import com.example.ui.onboarding.screens.PrescriptionReviewScreen
import com.example.ui.onboarding.screens.RoleSelectionScreen
import com.example.ui.onboarding.screens.WelcomeScreen

@Composable
fun OnboardingFlowScreen(
    viewModel: OnboardingViewModel,
    onFinishToMainApp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Handle Android system back button
    BackHandler(enabled = state.currentStep != OnboardingStep.WELCOME) {
        viewModel.handleBackNavigation()
    }

    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "onboarding_step_transition",
        modifier = modifier.fillMaxSize()
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> {
                WelcomeScreen(
                    onGetStartedClicked = { viewModel.onGetStartedClicked() },
                    onAlreadyHaveAccountClicked = { viewModel.onAlreadyHaveAccountClicked() }
                )
            }

            OnboardingStep.AUTH -> {
                AuthScreen(
                    state = state,
                    onAuthModeChanged = { mode -> viewModel.setAuthMode(mode) },
                    onNameChanged = { name -> viewModel.updateAuthName(name) },
                    onEmailChanged = { email -> viewModel.updateAuthEmail(email) },
                    onPhoneChanged = { phone -> viewModel.updateAuthPhone(phone) },
                    onPasswordChanged = { pass -> viewModel.updateAuthPassword(pass) },
                    onConfirmPasswordChanged = { confirm -> viewModel.updateAuthConfirmPassword(confirm) },
                    onSubmitAuth = {
                        viewModel.submitAuth(onExistingUserLoggedIn = {
                            onFinishToMainApp?.invoke()
                        })
                    },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.ROLE -> {
                RoleSelectionScreen(
                    selectedRole = state.selectedRole,
                    onRoleSelected = { role -> viewModel.selectRole(role) },
                    onContinueClicked = { viewModel.submitRole() },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.PROFILE -> {
                if (state.selectedRole == UserRole.PATIENT) {
                    PatientProfileScreen(
                        state = state,
                        onNameChanged = { viewModel.updatePatientName(it) },
                        onAgeChanged = { viewModel.updatePatientAge(it) },
                        onGenderChanged = { viewModel.updatePatientGender(it) },
                        onPhoneChanged = { viewModel.updatePatientPhone(it) },
                        onEmailChanged = { viewModel.updatePatientEmail(it) },
                        onDobChanged = { viewModel.updatePatientDob(it) },
                        onAddressChanged = { viewModel.updatePatientAddress(it) },
                        onEmergencyContactChanged = { viewModel.updatePatientEmergencyContact(it) },
                        onNotesChanged = { viewModel.updatePatientNotes(it) },
                        onContinueClicked = { viewModel.submitPatientProfile() },
                        onBackClicked = { viewModel.handleBackNavigation() }
                    )
                } else {
                    CaregiverProfileScreen(
                        state = state,
                        onCaregiverNameChanged = { viewModel.updateCaregiverName(it) },
                        onCaregiverAgeChanged = { viewModel.updateCaregiverAge(it) },
                        onCaregiverPhoneChanged = { viewModel.updateCaregiverPhone(it) },
                        onCaregiverEmailChanged = { viewModel.updateCaregiverEmail(it) },
                        onCaregiverRelationshipChanged = { viewModel.updateCaregiverRelationship(it) },
                        onPatientNameChanged = { viewModel.updatePatientName(it) },
                        onPatientAgeChanged = { viewModel.updatePatientAge(it) },
                        onPatientDobChanged = { viewModel.updatePatientDob(it) },
                        onPatientPhoneChanged = { viewModel.updatePatientPhone(it) },
                        onPatientNotesChanged = { viewModel.updatePatientNotes(it) },
                        onSubmitSubStep0 = { viewModel.submitCaregiverSubStep0() },
                        onSubmitSubStep1 = { viewModel.submitCaregiverSubStep1() },
                        onBackClicked = { viewModel.handleBackNavigation() }
                    )
                }
            }

            OnboardingStep.ADD_PRESCRIPTION -> {
                AddPrescriptionOptionScreen(
                    onScanClicked = { viewModel.onChooseScanPrescription() },
                    onManualClicked = { viewModel.onChooseManualPrescription() },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.CAMERA_SCAN -> {
                OnboardingCameraScanScreen(
                    isLoading = state.isLoading,
                    loadingMessage = state.ocrProcessingMessage,
                    onProcessBitmap = { bitmap -> viewModel.processOcrBitmap(bitmap) },
                    onProcessUri = { uri -> viewModel.processOcrUri(uri) },
                    onLoadSample = { sampleText -> viewModel.loadSamplePrescription(sampleText) },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.MANUAL_ENTRY -> {
                ManualMedicineEntryScreen(
                    state = state,
                    onSaveDraft = { draft -> viewModel.addOrUpdateDraftMedication(draft) },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.REVIEW -> {
                PrescriptionReviewScreen(
                    state = state,
                    onEditMedicine = { medId -> viewModel.editDraftMedication(medId) },
                    onDeleteMedicine = { medId -> viewModel.deleteDraftMedication(medId) },
                    onAddAnotherViaScan = { viewModel.onChooseScanPrescription() },
                    onAddAnotherViaManual = { viewModel.onChooseManualPrescription() },
                    onConfirmPrescription = {
                        viewModel.confirmPrescription {
                            // On confirmed
                        }
                    },
                    onBackClicked = { viewModel.handleBackNavigation() }
                )
            }

            OnboardingStep.FINISH -> {
                OnboardingFinishScreen(
                    state = state,
                    onContinueToApp = {
                        if (onFinishToMainApp != null) {
                            onFinishToMainApp()
                        } else {
                            // Default handoff placeholder screen
                        }
                    }
                )
            }
        }
    }
}
