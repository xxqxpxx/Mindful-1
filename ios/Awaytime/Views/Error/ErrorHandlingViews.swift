import SwiftUI

// MARK: - Error Alert View

struct ErrorAlertView: View {
    let error: ErrorHandlingService.AwayTimeError
    let onDismiss: () -> Void
    let onRecovery: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            // Error icon and title
            VStack(spacing: 12) {
                Text(error.icon)
                    .font(.system(size: 50))
                
                Text(error.errorDescription ?? "Error")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .multilineTextAlignment(.center)
            }
            
            // Error description
            if let failureReason = error.failureReason {
                Text(failureReason)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            
            // Recovery suggestion
            if let recoverySuggestion = error.recoverySuggestion {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "lightbulb")
                            .foregroundColor(.purple)
                        Text("How to fix this:")
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    
                    Text(recoverySuggestion)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(16)
                .background(Color.purple.opacity(0.1))
                .cornerRadius(12)
            }
            
            // Action buttons
            VStack(spacing: 12) {
                Button(action: onRecovery) {
                    HStack {
                        Image(systemName: "wrench.and.screwdriver")
                        Text(getRecoveryButtonText(for: error))
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                
                Button("Dismiss", action: onDismiss)
                    .foregroundColor(.secondary)
            }
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(20)
        .shadow(radius: 20)
        .padding(32)
    }
    
    private func getRecoveryButtonText(for error: ErrorHandlingService.AwayTimeError) -> String {
        switch error {
        case .permissionDenied, .permissionRevoked:
            return "Open Settings"
        case .blockingFailed:
            return "Try Again"
        case .dataCorruption:
            return "Fix Data"
        case .networkUnavailable:
            return "Retry"
        case .subscriptionError:
            return "Manage Subscription"
        case .deviceActivityError, .familyControlsError:
            return "Restart Service"
        case .storageError:
            return "Free Up Space"
        case .unknownError:
            return "Restart App"
        }
    }
}

// MARK: - Data Recovery Options View

struct DataRecoveryView: View {
    let onSafeReset: () -> Void
    let onFullReset: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 12) {
                Text("💾")
                    .font(.system(size: 50))
                
                Text("Data Recovery Options")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Choose how you'd like to fix your data:")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            // Recovery options
            VStack(spacing: 16) {
                RecoveryOptionCard(
                    icon: "arrow.clockwise.circle",
                    title: "Safe Reset",
                    description: "Fix corrupted data while keeping your app selections and goals",
                    recommended: true,
                    action: onSafeReset
                )
                
                RecoveryOptionCard(
                    icon: "trash.circle",
                    title: "Full Reset",
                    description: "Clear all data and start fresh (you'll need to set up again)",
                    recommended: false,
                    action: onFullReset
                )
            }
            
            // Cancel button
            Button("Cancel", action: onCancel)
                .foregroundColor(.secondary)
                .padding(.top)
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(20)
        .shadow(radius: 20)
        .padding(32)
    }
}

struct RecoveryOptionCard: View {
    let icon: String
    let title: String
    let description: String
    let recommended: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.purple)
                    .frame(width: 30)
                
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(title)
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        if recommended {
                            Text("RECOMMENDED")
                                .font(.caption)
                                .fontWeight(.bold)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }
                    }
                    
                    Text(description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(16)
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Error Toast View

struct ErrorToastView: View {
    let error: ErrorHandlingService.AwayTimeError
    @State private var isVisible = false
    
    var body: some View {
        VStack {
            Spacer()
            
            HStack(spacing: 12) {
                Text(error.icon)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(error.errorDescription ?? "Error")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                    
                    if let failureReason = error.failureReason {
                        Text(failureReason)
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                            .lineLimit(2)
                    }
                }
                
                Spacer()
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(error.severity.color.opacity(0.9))
                    .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
            )
            .padding(.horizontal, 16)
            .offset(y: isVisible ? 0 : 100)
            .opacity(isVisible ? 1 : 0)
        }
        .onAppear {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                isVisible = true
            }
            
            // Auto-hide based on severity
            let hideDelay: TimeInterval = error.severity == .critical ? 8 : 4
            DispatchQueue.main.asyncAfter(deadline: .now() + hideDelay) {
                withAnimation(.easeOut(duration: 0.3)) {
                    isVisible = false
                }
            }
        }
    }
}

// MARK: - Permission Guide View

struct PermissionGuideView: View {
    let permissionType: ErrorHandlingService.PermissionType
    let onOpenSettings: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 12) {
                Text("🔒")
                    .font(.system(size: 50))
                
                Text("Permission Required")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Awaytime needs \(permissionType.displayName) permission to help you stay focused.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            // Step-by-step guide
            VStack(alignment: .leading, spacing: 16) {
                Text("Follow these steps:")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                ForEach(getSteps(for: permissionType), id: \.number) { step in
                    PermissionStepView(step: step)
                }
            }
            .padding(20)
            .background(Color(.systemGray6))
            .cornerRadius(16)
            
            // Action buttons
            VStack(spacing: 12) {
                Button(action: onOpenSettings) {
                    HStack {
                        Image(systemName: "gear")
                        Text("Open Settings")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                
                Button("I'll Do This Later", action: onDismiss)
                    .foregroundColor(.secondary)
            }
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(20)
        .shadow(radius: 20)
        .padding(32)
    }
    
    private func getSteps(for type: ErrorHandlingService.PermissionType) -> [PermissionStep] {
        switch type {
        case .familyControls:
            return [
                PermissionStep(number: 1, text: "Tap 'Open Settings' below"),
                PermissionStep(number: 2, text: "Go to Screen Time"),
                PermissionStep(number: 3, text: "Tap 'Content & Privacy Restrictions'"),
                PermissionStep(number: 4, text: "Find Awaytime and enable it")
            ]
        case .notifications:
            return [
                PermissionStep(number: 1, text: "Tap 'Open Settings' below"),
                PermissionStep(number: 2, text: "Find Awaytime in the list"),
                PermissionStep(number: 3, text: "Tap 'Notifications'"),
                PermissionStep(number: 4, text: "Enable 'Allow Notifications'")
            ]
        case .screenTime:
            return [
                PermissionStep(number: 1, text: "Tap 'Open Settings' below"),
                PermissionStep(number: 2, text: "Go to Screen Time"),
                PermissionStep(number: 3, text: "Ensure Screen Time is enabled"),
                PermissionStep(number: 4, text: "Grant Awaytime access")
            ]
        }
    }
}

struct PermissionStep {
    let number: Int
    let text: String
}

struct PermissionStepView: View {
    let step: PermissionStep
    
    var body: some View {
        HStack(spacing: 12) {
            Text("\(step.number)")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(Color.purple)
                .cornerRadius(12)
            
            Text(step.text)
                .font(.subheadline)
            
            Spacer()
        }
    }
}

// MARK: - Error Status Banner

struct ErrorStatusBanner: View {
    let error: ErrorHandlingService.AwayTimeError
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Text(error.icon)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(error.errorDescription ?? "Error")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Text("Tap to resolve")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(12)
            .background(error.severity.color.opacity(0.1))
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(error.severity.color.opacity(0.3), lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Loading State with Error Fallback

struct LoadingWithErrorView<Content: View>: View {
    let isLoading: Bool
    let error: ErrorHandlingService.AwayTimeError?
    let onRetry: () -> Void
    let content: () -> Content
    
    var body: some View {
        Group {
            if isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                        .tint(.purple)
                    
                    Text("Loading...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = error {
                VStack(spacing: 20) {
                    Text(error.icon)
                        .font(.system(size: 40))
                    
                    Text(error.errorDescription ?? "Error")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    if let failureReason = error.failureReason {
                        Text(failureReason)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    
                    Button("Try Again", action: onRetry)
                        .buttonStyle(.borderedProminent)
                        .tint(.purple)
                }
                .padding(40)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                content()
            }
        }
    }
}

// MARK: - Preview Helpers

#Preview("Error Alert") {
    ErrorAlertView(
        error: .permissionDenied(.familyControls),
        onDismiss: {},
        onRecovery: {}
    )
}

#Preview("Data Recovery") {
    DataRecoveryView(
        onSafeReset: {},
        onFullReset: {},
        onCancel: {}
    )
}

#Preview("Permission Guide") {
    PermissionGuideView(
        permissionType: .familyControls,
        onOpenSettings: {},
        onDismiss: {}
    )
}