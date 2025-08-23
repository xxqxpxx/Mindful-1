import SwiftUI

struct PermissionRequestView: View {
    let onPermissionGranted: () -> Void
    @StateObject private var permissionService = PermissionService()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 32) {
                VStack(spacing: 16) {
                    Image(systemName: "lock.shield")
                        .font(.system(size: 60))
                        .foregroundColor(.purple)
                    
                    Text("Permission Required")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Awaytime needs Screen Time permission to monitor your app usage and help you stay on track")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                
                VStack(spacing: 16) {
                    PermissionRow(
                        icon: "hourglass",
                        title: "Screen Time Access",
                        description: "Monitor app usage and set limits",
                        isGranted: permissionService.authorizationStatus == .approved
                    )
                    
                    if !permissionService.isIOSVersionSupported {
                        VStack(spacing: 8) {
                            Text("⚠️ iOS 16+ Required")
                                .font(.headline)
                                .foregroundColor(.orange)
                            
                            Text("Full Screen Time features require iOS 16.0 or later. Some features may be limited on your current iOS version.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(8)
                    }
                }
                
                Button(action: {
                    Task {
                        await permissionService.requestPermission()
                        if permissionService.authorizationStatus == .approved {
                            onPermissionGranted()
                        }
                    }
                }) {
                    Text(permissionService.authorizationStatus == .approved ? "Continue" : "Grant Permission")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.purple)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
            .navigationTitle("Permissions")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct PermissionRow: View {
    let icon: String
    let title: String
    let description: String
    let isGranted: Bool
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.purple)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: isGranted ? "checkmark.circle.fill" : "circle")
                .font(.title2)
                .foregroundColor(isGranted ? .green : .gray)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

#Preview {
    PermissionRequestView(onPermissionGranted: {})
}