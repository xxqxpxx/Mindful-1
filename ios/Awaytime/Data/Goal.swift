import Foundation

// Temporary Goal struct for compilation purposes
// This should be replaced with proper Core Data entity later
struct Goal {
    var id: UUID = UUID()
    var appIdentifier: String = ""
    var isLimitExceeded: Bool = false
    var dailyLimitMinutes: Int = 0
    var createdAt: Date = Date()
}
