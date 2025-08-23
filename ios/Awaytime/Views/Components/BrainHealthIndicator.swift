import SwiftUI

struct BrainHealthIndicator: View {
    let screenTimeHours: Double
    let motivationAlignment: Double
    let brainStressLevel: Int
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Brain Health")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(healthDescription)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Text(healthEmoji)
                    .font(.system(size: 32))
            }
            
            // Health meter
            HStack(spacing: 8) {
                ForEach(1...5, id: \.self) { level in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(level <= healthLevel ? healthColor : Color.gray.opacity(0.3))
                        .frame(height: 6)
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
        )
    }
    
    private var healthLevel: Int {
        if screenTimeHours <= 2 { return 5 }
        else if screenTimeHours <= 4 { return 4 }
        else if screenTimeHours <= 6 { return 3 }
        else if screenTimeHours <= 8 { return 2 }
        else { return 1 }
    }
    
    private var healthColor: Color {
        switch healthLevel {
        case 5, 4: return .green
        case 3: return .yellow
        case 2, 1: return .red
        default: return .gray
        }
    }
    
    private var healthDescription: String {
        switch healthLevel {
        case 5: return "Excellent digital wellness"
        case 4: return "Good balance maintained"
        case 3: return "Moderate usage levels"
        case 2: return "High usage detected"
        case 1: return "Consider taking breaks"
        default: return "Unknown"
        }
    }
    
    private var healthEmoji: String {
        switch healthLevel {
        case 5: return "🌟"
        case 4: return "😊"
        case 3: return "😐"
        case 2: return "😟"
        case 1: return "😰"
        default: return "🤔"
        }
    }
}