package example.Controller;

import example.Model.TransactionService;
import example.Model.TransactionService.DashboardStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin
public class DashboardController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    @GetMapping("/dashboard-stats")
    @ResponseBody
    public DashboardStats getDashboardStats(@org.springframework.web.bind.annotation.RequestParam(value = "date", required = false) String date)
            throws Exception {
        if (date == null || date.isEmpty()) {
            return transactionService.getTodayDashboardStats();
        }
        return transactionService.getDashboardStatsForDate(date);
    }
}


