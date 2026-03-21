package com.financetracker.config;

import com.financetracker.entity.Account;
import com.financetracker.entity.AccountType;
import com.financetracker.entity.Budget;
import com.financetracker.entity.Category;
import com.financetracker.entity.CategoryType;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.Goal;
import com.financetracker.entity.GoalStatus;
import com.financetracker.entity.RecurringFrequency;
import com.financetracker.entity.RecurringTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.BudgetRepository;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.GoalRepository;
import com.financetracker.repository.RecurringTransactionRepository;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDemoUser();
    }

    private void seedDemoUser() {
        if (userRepository.findByEmail("demo@financetracker.app").isPresent()) {
            return;
        }

        User user = User.builder()
                .email("demo@financetracker.app")
                .passwordHash(passwordEncoder.encode("DemoPass123"))
                .displayName("Demo User")
                .build();
        user = userRepository.save(user);

        Account bank = accountRepository.save(Account.builder()
                .user(user)
                .name("Primary Bank")
                .type(AccountType.BANK)
                .institutionName("Demo Bank")
                .openingBalance(new BigDecimal("5000.00"))
                .currentBalance(new BigDecimal("8250.00"))
                .build());

        Category food = createCategory(user, "Food", CategoryType.EXPENSE, "#3b82f6", "utensils");
        Category rent = createCategory(user, "Rent", CategoryType.EXPENSE, "#ef4444", "home");
        Category transport = createCategory(user, "Transport", CategoryType.EXPENSE, "#f59e0b", "car");
        Category subscriptions = createCategory(user, "Subscriptions", CategoryType.EXPENSE, "#8b5cf6", "repeat");
        Category salary = createCategory(user, "Salary", CategoryType.INCOME, "#10b981", "wallet");
        Category freelance = createCategory(user, "Freelance", CategoryType.INCOME, "#14b8a6", "briefcase");
        categoryRepository.saveAll(List.of(food, rent, transport, subscriptions, salary, freelance));

        LocalDate now = LocalDate.now();
        transactionRepository.saveAll(List.of(
                seedTransaction(user, bank, salary, TransactionType.INCOME, "3200.00", now.withDayOfMonth(1), "Employer Inc", "Monthly salary"),
                seedTransaction(user, bank, food, TransactionType.EXPENSE, "245.75", now.withDayOfMonth(Math.min(4, now.lengthOfMonth())), "Fresh Basket", "Groceries"),
                seedTransaction(user, bank, transport, TransactionType.EXPENSE, "82.40", now.withDayOfMonth(Math.min(7, now.lengthOfMonth())), "Metro Card", "Commute top-up"),
                seedTransaction(user, bank, freelance, TransactionType.INCOME, "600.00", now.withDayOfMonth(Math.min(9, now.lengthOfMonth())), "Client Project", "Freelance invoice"),
                seedTransaction(user, bank, subscriptions, TransactionType.EXPENSE, "19.99", now.withDayOfMonth(Math.min(12, now.lengthOfMonth())), "Netflix", "Monthly subscription")
        ));

        budgetRepository.saveAll(List.of(
                Budget.builder().user(user).category(food).month(now.getMonthValue()).year(now.getYear()).amount(new BigDecimal("700.00")).alertThresholdPercent(80).build(),
                Budget.builder().user(user).category(transport).month(now.getMonthValue()).year(now.getYear()).amount(new BigDecimal("250.00")).alertThresholdPercent(80).build(),
                Budget.builder().user(user).category(subscriptions).month(now.getMonthValue()).year(now.getYear()).amount(new BigDecimal("60.00")).alertThresholdPercent(90).build()
        ));

        goalRepository.saveAll(List.of(
                Goal.builder().user(user).name("Emergency Fund").targetAmount(new BigDecimal("100000.00")).currentAmount(new BigDecimal("45000.00")).targetDate(now.plusMonths(9)).linkedAccount(bank).icon("shield").color("#3563e9").status(GoalStatus.ACTIVE).build(),
                Goal.builder().user(user).name("Vacation").targetAmount(new BigDecimal("50000.00")).currentAmount(new BigDecimal("20000.00")).targetDate(now.plusMonths(5)).linkedAccount(bank).icon("plane").color("#14b8a6").status(GoalStatus.ACTIVE).build()
        ));

        recurringTransactionRepository.saveAll(List.of(
                RecurringTransaction.builder().user(user).title("Netflix").type(TransactionType.EXPENSE).amount(new BigDecimal("19.99")).category(subscriptions).account(bank).frequency(RecurringFrequency.MONTHLY).startDate(now.minusMonths(2)).nextRunDate(now.plusDays(2)).autoCreateTransaction(true).paused(false).build(),
                RecurringTransaction.builder().user(user).title("Monthly Salary").type(TransactionType.INCOME).amount(new BigDecimal("3200.00")).category(salary).account(bank).frequency(RecurringFrequency.MONTHLY).startDate(now.minusMonths(6)).nextRunDate(now.plusDays(12)).autoCreateTransaction(true).paused(false).build()
        ));
    }

    private Category createCategory(User user, String name, CategoryType type, String color, String icon) {
        return Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .color(color)
                .icon(icon)
                .isArchived(false)
                .build();
    }

    private FinanceTransaction seedTransaction(User user, Account account, Category category, TransactionType type, String amount, LocalDate date, String merchant, String note) {
        return FinanceTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(type)
                .amount(new BigDecimal(amount))
                .transactionDate(date)
                .merchant(merchant)
                .note(note)
                .paymentMethod("CARD")
                .build();
    }
}
