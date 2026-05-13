---
layout: home
title: loyalty-ledger
hero:
  name: loyalty-ledger
  text: Event-driven point ledger
  tagline: A read-side service that turns order events into a per-customer point ledger and exposes it as a small, versioned REST surface.
  actions:
    - theme: brand
      text: QuickStart
      link: /quickstart/
    - theme: alt
      text: Architecture overview
      link: /architecture/
features:
  - title: QuickStart
    details: Clone, bootstrap the toolchain, run the canonical Make targets, render the docs locally.
    link: /quickstart/
    linkText: Get set up
  - title: Architecture
    details: Service purpose, boundaries, read/write flows, and links into C4+1, DDD, REST, Events.
    link: /architecture/
    linkText: Read the overview
  - title: API (sync)
    details: Read-only HTTP surface — current balance and paged movements per customer.
    link: /api/
    linkText: Open the Swagger UI
  - title: Events (async)
    details: Inbound order topics, outbound balance-update topic, idempotency, outbox.
    link: /events/
    linkText: Open the AsyncAPI viewer
  - title: ADRs
    details: Curated index of architectural decisions with featured records and links to source.
    link: /adr/
    linkText: Browse decisions
  - title: FAQ
    details: Quick answers to common contributor and reviewer questions.
    link: /faq/
    linkText: Open the FAQ
  - title: Acceptance Report
    details: Latest Cucumber acceptance-test report rendered with Allure.
    link: https://lglabs-loyalty-allure.web.app/
    linkText: Open Allure (external)
  - title: Onboarding Runbook
    details: First-day checklist and operational orientation for new contributors.
    link: /runbook/
    linkText: Open the runbook
---
