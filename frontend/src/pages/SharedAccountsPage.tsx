import { useEffect, useMemo, useState } from "react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import SectionCard from "../components/SectionCard";
import StatCard from "../components/StatCard";
import { useToast } from "../context/ToastContext";
import { financeService } from "../services/financeService";
import { Account, AccountActivityFeed, AccountMemberList } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";

function Field({ label, required = false, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="grid gap-2 text-sm font-medium text-slate-700">
      <span>
        {label}
        {required ? <span className="ml-1 text-rose-600">*</span> : null}
      </span>
      {children}
    </label>
  );
}

export default function SharedAccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [membersData, setMembersData] = useState<AccountMemberList | null>(null);
  const [activityFeed, setActivityFeed] = useState<AccountActivityFeed | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<"EDITOR" | "VIEWER">("EDITOR");
  const [loadingMembers, setLoadingMembers] = useState(false);
  const { showToast } = useToast();

  const loadAccounts = () =>
    financeService.getAccounts().then((items) => {
      setAccounts(items);
      if (!selectedAccountId && items.length) {
        const preferred = items.find((account) => account.accessRole === "OWNER") || items[0];
        setSelectedAccountId(preferred.id);
      }
    }).catch((error) => showToast(getApiErrorMessage(error, "Unable to load accounts"), "error"));

  useEffect(() => {
    loadAccounts();
  }, []);

  useEffect(() => {
    if (!selectedAccountId) return;
    setLoadingMembers(true);
    Promise.allSettled([
      financeService.getAccountMembers(selectedAccountId),
      financeService.getAccountActivity(selectedAccountId),
    ]).then(([membersResult, activityResult]) => {
      if (membersResult.status === "fulfilled") {
        setMembersData(membersResult.value);
      } else {
        setMembersData(null);
        showToast(getApiErrorMessage(membersResult.reason, "Unable to load members"), "error");
      }
      if (activityResult.status === "fulfilled") {
        setActivityFeed(activityResult.value);
      } else {
        setActivityFeed(null);
        showToast(getApiErrorMessage(activityResult.reason, "Unable to load account activity"), "error");
      }
    }).finally(() => setLoadingMembers(false));
  }, [selectedAccountId]);

  const selectedAccount = useMemo(
    () => accounts.find((account) => account.id === selectedAccountId) || null,
    [accounts, selectedAccountId],
  );

  const ownerAccounts = accounts.filter((account) => account.accessRole === "OWNER");
  const sharedWithYouCount = accounts.filter((account) => account.accessRole && account.accessRole !== "OWNER").length;
  const canManageMembers = selectedAccount?.accessRole === "OWNER";

  const refreshSelection = async () => {
    if (!selectedAccountId) return;
    setMembersData(await financeService.getAccountMembers(selectedAccountId));
    setActivityFeed(await financeService.getAccountActivity(selectedAccountId));
  };

  const invite = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedAccountId || !inviteEmail.trim()) {
      showToast("Select an account and enter an email", "error");
      return;
    }
    try {
      await financeService.inviteAccountMember(selectedAccountId, { email: inviteEmail.trim(), role: inviteRole });
      showToast("Member added to shared account");
      setInviteEmail("");
      await refreshSelection();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to invite member"), "error");
    }
  };

  const updateRole = async (userId: string, role: "OWNER" | "EDITOR" | "VIEWER") => {
    if (!selectedAccountId) return;
    try {
      await financeService.updateAccountMember(selectedAccountId, userId, { role });
      showToast("Member role updated");
      await refreshSelection();
      loadAccounts();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to update member role"), "error");
    }
  };

  return (
    <AppShell title="Shared Accounts">
      <div className="grid gap-4 md:grid-cols-3">
        <StatCard label="Accessible accounts" value={String(accounts.length)} helper="Accounts you can view or manage" />
        <StatCard label="Owned by you" value={String(ownerAccounts.length)} helper="Accounts where you can invite members" />
        <StatCard label="Shared with you" value={String(sharedWithYouCount)} helper="Family or collaborator access" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[360px_1fr]">
        <SectionCard title="Invite access" eyebrow="Family mode">
          <form className="grid gap-4" onSubmit={invite}>
            <Field label="Account" required>
              <select value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)} className="app-select">
                <option value="">Choose account</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name} {account.accessRole ? `(${account.accessRole})` : ""}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Invite by email" required>
              <input
                value={inviteEmail}
                onChange={(event) => setInviteEmail(event.target.value)}
                placeholder="family@example.com"
                className="app-input"
                disabled={!canManageMembers}
              />
            </Field>
            <Field label="Role" required>
              <select
                value={inviteRole}
                onChange={(event) => setInviteRole(event.target.value as "EDITOR" | "VIEWER")}
                className="app-select"
                disabled={!canManageMembers}
              >
                <option value="EDITOR">Editor</option>
                <option value="VIEWER">Viewer</option>
              </select>
            </Field>
            <p className="text-sm text-slate-500">
              {canManageMembers
                ? "Owners can invite collaborators, change their roles, and review account activity."
                : selectedAccount
                  ? `This account is shared with you as ${selectedAccount.accessRole}. Only the owner can manage members.`
                  : "Choose an account to view or manage access."}
            </p>
            <button className="app-button-primary" disabled={!canManageMembers}>
              Send invite
            </button>
          </form>
        </SectionCard>

        <div className="grid gap-6">
          <SectionCard title="Members and roles" eyebrow="Access control">
            {!selectedAccount ? (
              <EmptyState title="No account selected" message="Choose an account to review shared access." />
            ) : loadingMembers ? (
              <div className="space-y-3">
                <div className="h-20 animate-pulse rounded-2xl bg-slate-100" />
                <div className="h-20 animate-pulse rounded-2xl bg-slate-100" />
              </div>
            ) : membersData?.members.length ? (
              <div className="space-y-4">
                <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
                  Sharing for <span className="font-semibold text-slate-900">{membersData.accountName}</span>
                </div>
                {membersData.members.map((member) => (
                  <div key={member.userId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4">
                    <div>
                      <p className="font-semibold text-slate-900">{member.displayName}</p>
                      <p className="mt-1 text-sm text-slate-500">{member.email}</p>
                    </div>
                    {canManageMembers && member.role !== "OWNER" ? (
                      <select
                        value={member.role}
                        onChange={(event) => updateRole(member.userId, event.target.value as "OWNER" | "EDITOR" | "VIEWER")}
                        className="app-select w-40"
                      >
                        <option value="EDITOR">Editor</option>
                        <option value="VIEWER">Viewer</option>
                      </select>
                    ) : (
                      <span className="rounded-full bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-700">{member.role}</span>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState title="No members yet" message="Invite a family member or collaborator to start sharing this account." />
            )}
          </SectionCard>

          <SectionCard title="Recent shared activity" eyebrow="Audit trail">
            {!selectedAccount ? (
              <EmptyState title="No account selected" message="Choose an account to review recent activity." />
            ) : loadingMembers ? (
              <div className="space-y-3">
                <div className="h-20 animate-pulse rounded-2xl bg-slate-100" />
                <div className="h-20 animate-pulse rounded-2xl bg-slate-100" />
              </div>
            ) : activityFeed?.activities.length ? (
              <div className="space-y-3">
                {activityFeed.activities.map((activity) => (
                  <div key={activity.id} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="font-semibold text-slate-900">{activity.actorName} • {activity.action}</p>
                        <p className="mt-1 text-sm text-slate-500">{activity.resourceType} • {activity.resourceName}</p>
                        {activity.details ? <p className="mt-2 text-sm text-slate-600">{activity.details}</p> : null}
                      </div>
                      <span className="text-xs text-slate-400">{activity.createdAt?.replace("T", " ").slice(0, 16)}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState title="No activity yet" message="Recent collaboration and account actions will appear here." />
            )}
          </SectionCard>
        </div>
      </div>
    </AppShell>
  );
}
