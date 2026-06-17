import 'package:go_router/go_router.dart';

import '../editor/editor_page.dart';
import '../projects/project_list_page.dart';

/// App routes: project list at `/`, editor at `/editor/:id`.
final appRouter = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const ProjectListPage(),
    ),
    GoRoute(
      path: '/editor/:id',
      builder: (context, state) =>
          EditorPage(projectId: state.pathParameters['id']!),
    ),
  ],
);
