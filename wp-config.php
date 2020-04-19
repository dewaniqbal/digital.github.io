<?php
/**
 * The base configuration for WordPress
 *
 * The wp-config.php creation script uses this file during the
 * installation. You don't have to use the web site, you can
 * copy this file to "wp-config.php" and fill in the values.
 *
 * This file contains the following configurations:
 *
 * * MySQL settings
 * * Secret keys
 * * Database table prefix
 * * ABSPATH
 *
 * @link https://codex.wordpress.org/Editing_wp-config.php
 *
 * @package WordPress
 */

// ** MySQL settings - You can get this info from your web host ** //
/** The name of the database for WordPress */
define( 'DB_NAME', 'digital' );

/** MySQL database username */
define( 'DB_USER', 'root' );

/** MySQL database password */
define( 'DB_PASSWORD', '' );

/** MySQL hostname */
define( 'DB_HOST', 'localhost' );

/** Database Charset to use in creating database tables. */
define( 'DB_CHARSET', 'utf8mb4' );

/** The Database Collate type. Don't change this if in doubt. */
define( 'DB_COLLATE', '' );

/**#@+
 * Authentication Unique Keys and Salts.
 *
 * Change these to different unique phrases!
 * You can generate these using the {@link https://api.wordpress.org/secret-key/1.1/salt/ WordPress.org secret-key service}
 * You can change these at any point in time to invalidate all existing cookies. This will force all users to have to log in again.
 *
 * @since 2.6.0
 */
define( 'AUTH_KEY',         '$f[UA,g+Q|u0)vL#_EK2UClgnICzND`+i?w@k9? Cxz1!+~E*0v|j,u)|h.d^t`W' );
define( 'SECURE_AUTH_KEY',  'y-FBrrT&979lp4}vbm$yyb#?}i[ =Wuel-Z%P^eZNW9jVGM/R^5U89K3C+7_aI,0' );
define( 'LOGGED_IN_KEY',    '@oLx4WslM=x;6l~h{d18Wv{lag/@1mGzB>HbRUqLO|uV{|5{]X`4#{Su+&&tU.;7' );
define( 'NONCE_KEY',        'B+XnfM6By[bb~<_h*!OEGYMf}.6-KpD)]~]BQ*!{A|L:BIv<>Hv?qC04R-Y]?:@U' );
define( 'AUTH_SALT',        'J^6M?Df1R#w*X>h{8-T<b@qm$Twe^DFFE]TT@krYfkWd$J;i^wkUFaz4nnAY}(C ' );
define( 'SECURE_AUTH_SALT', 'S$KW1@pj$oh;/in06MqC3x22*717{]cYX;R&S=I-jxi$s1wQe.H&)a.VXD)44U6{' );
define( 'LOGGED_IN_SALT',   'r8_|!X33S8!~)H,B0mdm}[yg-V8~2~&E6TtAEe6#2p{va8{2CEWGtryBbzOjB%o>' );
define( 'NONCE_SALT',       '`F&uoCKL}SO)`gxV9jDV:K-bRzA[@WFf+~#k1bsMo4JlpXB0$>C.sxK^4_F QaC{' );

/**#@-*/

/**
 * WordPress Database Table prefix.
 *
 * You can have multiple installations in one database if you give each
 * a unique prefix. Only numbers, letters, and underscores please!
 */
$table_prefix = 'wp_digital';

/**
 * For developers: WordPress debugging mode.
 *
 * Change this to true to enable the display of notices during development.
 * It is strongly recommended that plugin and theme developers use WP_DEBUG
 * in their development environments.
 *
 * For information on other constants that can be used for debugging,
 * visit the Codex.
 *
 * @link https://codex.wordpress.org/Debugging_in_WordPress
 */
define( 'WP_DEBUG', false );

/* That's all, stop editing! Happy publishing. */

/** Absolute path to the WordPress directory. */
if ( ! defined( 'ABSPATH' ) ) {
	define( 'ABSPATH', dirname( __FILE__ ) . '/' );
}

/** Sets up WordPress vars and included files. */
require_once( ABSPATH . 'wp-settings.php' );
