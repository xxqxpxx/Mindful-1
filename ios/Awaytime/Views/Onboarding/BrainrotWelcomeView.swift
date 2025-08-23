import SwiftUI

struct BrainrotWelcomeView: View {
    let onContinue: () -> Void
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 40) {
                Spacer()
                
                // Brain mascot
                BrainMascotView(state: .happy, size: 160)
                
                // Welcome text
                VStack(spacing: 16) {
                    Text("welcome to brainrot")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("it's time to regain control of your screen time")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                // Get started button
                Button(action: onContinue) {
                    Text("get started")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 28)
                                .fill(Color.blue)
                        )
                }
                .padding(.horizontal, 32)
                
                // Terms and privacy
                VStack(spacing: 8) {
                    Text("By continuing, you agree to our")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 16) {
                        Button("Terms of Service") {
                            // Handle terms tap
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                        
                        Text("and")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Button("Privacy Policy") {
                            // Handle privacy tap
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                    }
                }
                .padding(.bottom, 40)
            }
        }
    }
}

struct BrainrotIntroView: View {
    let onGetStarted: () -> Void
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(UIColor.systemBackground),
                    Color.blue.opacity(0.1)
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 0) {
                Spacer()
                
                // Mock iPhone with app icon
                ZStack {
                    // Phone outline
                    RoundedRectangle(cornerRadius: 25)
                        .stroke(Color.primary.opacity(0.2), lineWidth: 2)
                        .frame(width: 200, height: 400)
                    
                    // Screen content
                    VStack(spacing: 20) {
                        // Status bar mockup
                        HStack {
                            Text("2:23")
                                .font(.caption)
                                .fontWeight(.medium)
                            
                            Spacer()
                            
                            HStack(spacing: 2) {
                                ForEach(0..<4) { _ in
                                    Rectangle()
                                        .frame(width: 3, height: 3)
                                        .foregroundColor(.primary.opacity(0.6))
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 20)
                        
                        // App icons grid
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 16) {
                            ForEach(0..<16) { index in
                                if index == 10 { // Brainrot app position
                                    BrainMascotView(state: .happy, size: 32)
                                        .background(
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.blue.opacity(0.1))
                                                .frame(width: 40, height: 40)
                                        )
                                } else {
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(Color.gray.opacity(0.3))
                                        .frame(width: 32, height: 32)
                                }
                            }
                        }
                        .padding(.horizontal, 30)
                        
                        Spacer()
                    }
                }
                .padding(.bottom, 40)
                
                // Call to action
                VStack(spacing: 16) {
                    Text("stop scrolling.\nsave your brain.")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.primary)
                    
                    Text("Take control of your digital habits")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                // Get started button
                Button(action: onGetStarted) {
                    Text("get started")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 28)
                                .fill(Color.blue)
                        )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
        }
    }
}

struct BrainrotWelcomeView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            BrainrotWelcomeView() {}
            BrainrotIntroView() {}
        }
    }
}