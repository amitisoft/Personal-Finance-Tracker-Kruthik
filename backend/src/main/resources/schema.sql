create extension if not exists "pgcrypto";

create table if not exists users (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    email varchar(255) unique not null,
    password_hash varchar(255) not null,
    display_name varchar(120) not null
);

create table if not exists accounts (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    name varchar(100) not null,
    type varchar(30) not null,
    opening_balance numeric(12,2) not null default 0,
    current_balance numeric(12,2) not null default 0,
    institution_name varchar(120)
);

create table if not exists categories (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid references users(id),
    name varchar(100) not null,
    type varchar(20) not null,
    color varchar(20),
    icon varchar(50),
    is_archived boolean not null default false
);

create table if not exists transactions (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    account_id uuid not null references accounts(id),
    category_id uuid references categories(id),
    type varchar(20) not null,
    amount numeric(12,2) not null,
    transaction_date date not null,
    merchant varchar(200),
    note text,
    payment_method varchar(50),
    recurring_transaction_id uuid,
    transfer_account_id uuid
);

create table if not exists transaction_tags (
    transaction_id uuid not null references transactions(id),
    tag varchar(100) not null
);

create table if not exists budgets (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    category_id uuid not null references categories(id),
    month int not null,
    year int not null,
    amount numeric(12,2) not null,
    alert_threshold_percent int not null default 80
);

create table if not exists goals (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    name varchar(120) not null,
    target_amount numeric(12,2) not null,
    current_amount numeric(12,2) not null default 0,
    target_date date,
    linked_account_id uuid references accounts(id),
    icon varchar(50),
    color varchar(20),
    status varchar(30) not null
);

create table if not exists recurring_transactions (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    title varchar(120) not null,
    type varchar(20) not null,
    amount numeric(12,2) not null,
    category_id uuid references categories(id),
    account_id uuid references accounts(id),
    frequency varchar(20) not null,
    start_date date not null,
    end_date date,
    next_run_date date not null,
    auto_create_transaction boolean not null default true,
    paused boolean not null default false
);

create table if not exists refresh_tokens (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    user_id uuid not null references users(id),
    token varchar(500) unique not null,
    expires_at timestamp not null,
    revoked boolean not null default false
);
