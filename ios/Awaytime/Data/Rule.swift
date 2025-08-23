import Foundation

// Temporary Rule struct for compilation purposes
// This should be replaced with proper Core Data entity later
struct Rule {
    var id: UUID = UUID()
    var name: String = ""
    var isFirstRule: Bool = false
    var dailyLimitMinutes: Int32 = 0
    var createdAt: Date = Date()
    
    init() {}
}
