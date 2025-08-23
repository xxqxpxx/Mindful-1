
import SwiftUI
import DeviceActivity

extension DeviceActivityReport.Context {
    static let totalActivity = Self("TotalActivity")
}

struct TotalActivityReport: DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .totalActivity

    func makeConfiguration(representing data: DeviceActivityResults) async -> String {
        var totalActivityDuration: TimeInterval = 0
        for await activity in data.activitySegments {
            totalActivityDuration += activity.totalActivityDuration
        }
        
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        userDefaults?.set(totalActivityDuration, forKey: "totalActivityDuration")
        
        return "Updated"
    }
    
    var content: (String) -> some View {
        Text.init
    }
}
