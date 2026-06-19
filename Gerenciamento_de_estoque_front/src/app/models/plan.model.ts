export interface Plan {
  id: number;
  name: string;
  description: string;
  price: number;
  type: 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
  maxUsers?: number;
  maxProducts?: number;
  maxOrganizations?: number;
  hasReports?: boolean;
  hasAdvancedAnalytics?: boolean;
  hasApiAccess?: boolean;
  active?: boolean;
}
