import Foundation

struct UserMotivation {
    let id = UUID()
    let reasons: Set<Reason>
    let createdDate = Date()
    
    enum Reason: String, CaseIterable {
        case improveFocus = "improve_focus"
        case beMorePresent = "be_more_present"
        case betterSleep = "better_sleep"
        case increaseProductivity = "increase_productivity"
        case reduceAnxiety = "reduce_anxiety"
        case spendTimeWithFamily = "spend_time_with_family"
        case developHobbies = "develop_hobbies"
        case improveHealth = "improve_health"
        
        var displayName: String {
            switch self {
            case .improveFocus:
                return "Improve Focus"
            case .beMorePresent:
                return "Be More Present"
            case .betterSleep:
                return "Better Sleep"
            case .increaseProductivity:
                return "Increase Productivity"
            case .reduceAnxiety:
                return "Reduce Anxiety"
            case .spendTimeWithFamily:
                return "Spend Time with Family"
            case .developHobbies:
                return "Develop Hobbies"
            case .improveHealth:
                return "Improve Health"
            }
        }
        
        var icon: String {
            switch self {
            case .improveFocus:
                return "target"
            case .beMorePresent:
                return "person.2.fill"
            case .betterSleep:
                return "moon.fill"
            case .increaseProductivity:
                return "chart.line.uptrend.xyaxis"
            case .reduceAnxiety:
                return "heart.fill"
            case .spendTimeWithFamily:
                return "house.fill"
            case .developHobbies:
                return "paintbrush.fill"
            case .improveHealth:
                return "figure.walk"
            }
        }
    }
}