import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { set } from '../lib/api';

import { moduleURI } from '../lib/helpers';
import availableModules from '../lib/modules';
import Popup from '../components/Popup';
import Icon from '../components/Icon';

const Modules = ({ modules, updateModules }) => {
	const [activeModule, setModule] = useState(null);
	const [getInstall, setInstall] = useState({ error: null, loading: false, module: null });
	const [getRemove, setRemove] = useState({ error: null, loading: false, module: null });
	const [getUpdate, setUpdate] = useState({ error: null, loading: false, module: null });

	const installModule = async (e) => {
		e.preventDefault();

		setInstall({ ...getInstall, loading: true });

		const { error } = await set('addModule', { user_slash_repo: getInstall.module });

		if (!error) {
			updateModules();
			setInstall({ module: null, error: null, loading: false });
		} else {
			setInstall({ ...getInstall, loading: false, error });
		}
	};

	const updateModule = async () => {
		const { path, version } = getUpdate.module;

		setUpdate({ ...getUpdate, loading: true });

		const { error } = await set('updateModule', { path, version });

		if (!error) {
			updateModules();
			setUpdate({ module: null, error: null, loading: false });
		} else {
			setUpdate({ ...getUpdate, loading: false, error });
		}
	};

	const removeModule = async () => {
		setRemove({ ...getRemove, loading: true });

		const { error } = await set('removeModule', { path: getRemove.module });

		if (!error) {
			updateModules();
			setRemove({ module: null, error: null, loading: false });
		} else {
			setRemove({ ...getRemove, loading: false, error });
		}
	};

	const installedModules = modules.map(({ name }) => name);

	return (
		<section className="modules">
			<article>
				<h1>
					Manage Modules{' '}
					<nav>
						<button
							className="button secondary"
							onClick={() => setInstall({ ...getInstall, module: '' })}
							type="button"
						>
							Install third party module
						</button>
					</nav>
				</h1>

				{typeof getInstall.module === 'string' && (
					<Popup
						title="Install module"
						loading={getInstall.loading}
						onClose={() => setInstall({ module: null, error: null, loading: false })}
					>
						<form onSubmit={installModule}>
							<fieldset>
								<p>
									<strong>Do not install modules from untrusted sources!</strong>
									<br />
									Format: <strong>username/repository</strong> or Github URL
								</p>
								<label htmlFor="newAddress">
									GitHub repository
									<input
										type="text"
										id="newAddress"
										value={getInstall.module}
										onChange={(e) => setInstall({ ...getInstall, module: e.target.value })}
									/>
								</label>
								{getInstall.error && <small className="error">{getInstall.error}</small>}
								<button className="button" type="submit">
									Install
								</button>
							</fieldset>
						</form>
					</Popup>
				)}

				{getRemove.module && (
					<Popup
						onConfirm={removeModule}
						type="warning"
						cta="Remove"
						loading={getRemove.loading}
						onClose={() => setRemove({ module: null, loading: null, error: null })}
					>
						<p>
							Remove module <strong>{getRemove.module}</strong>?
							{getRemove.error && <small className="error">{getRemove.error}</small>}
						</p>
					</Popup>
				)}

				{getUpdate.module && (
					<Popup
						onConfirm={updateModule}
						type="success"
						cta="Update"
						loading={getUpdate.loading}
						onClose={() => setUpdate({ module: null, loading: null, error: null })}
					>
						<p>
							Update module <strong>{getUpdate.module.path}</strong> to version{' '}
							<strong>{getUpdate.module.version}</strong>?
							{getUpdate.error && <small className="error">{getUpdate.error}</small>}
						</p>
					</Popup>
				)}

				{modules.length > 0 && (
					<div className="module-list">
						<h3>Installed</h3>
						<ul>
							{modules.map(({ name, repository, update, path, description, gui_port }) => (
								<li
									onClick={() => setModule(activeModule !== name ? name : null)}
									key={path}
									className={activeModule === name ? 'active' : ''}
								>
									<h4>
										<Icon size={20} icon="info" />
										<Icon size={20} icon="close" />
										{name}
										<nav>
											{gui_port > -1 && (
												<a
													href={moduleURI(name, gui_port)}
													className="button success small"
													target="_blank"
													rel="noopener noreferrer"
												>
													Launch
												</a>
											)}
											{update && (
												<button
													className="button small"
													onClick={() => setUpdate({ ...update, module: { path, version: update } })}
													type="button"
												>
													Update
												</button>
											)}
											<button
												className="button warning small"
												onClick={() => setRemove({ ...getRemove, module: path })}
												type="button"
											>
												Remove
											</button>
										</nav>
									</h4>
									<p>{description}</p>
									<a href={`https://github.com/${repository}`} target="_blank" rel="noopener noreferrer">
										<Icon size={12} icon="link" />
										Visit GitHub Repo
									</a>
								</li>
							))}
						</ul>
					</div>
				)}
				<div className="module-list">
					<h3>Supported modules</h3>
					<ul>
						{availableModules.map(
							({ name, repo, description }) =>
								installedModules.indexOf(name) < 0 && (
									<li onClick={() => setModule(name)} key={repo} className={activeModule === name ? 'active' : ''}>
										<h4>
											<Icon size={20} icon="info" />
											<Icon size={20} icon="close" />
											{name}
											<nav>
												<button
													className="button small success"
													onClick={(e) => {
														e.stopPropagation();
														setInstall({ ...getInstall, module: repo });
													}}
													type="button"
												>
													Install
												</button>
											</nav>
										</h4>
										<p>{description}</p>
										<a
											href={`https://github.com/${repo}`}
											onClick={(e) => e.stopPropagation()}
											target="_blank"
											rel="noopener noreferrer"
										>
											<Icon size={12} icon="link" />
											Visit GitHub Repo
										</a>
									</li>
								)
						)}
					</ul>
				</div>
			</article>
		</section>
	);
};

Modules.propTypes = {
	modules: PropTypes.array.isRequired,
	updateModules: PropTypes.func.isRequired
};

export default Modules;
