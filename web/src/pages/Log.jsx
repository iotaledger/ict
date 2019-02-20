import React, { Component } from 'react';

import Card from '../components/Card';

import { get } from '../lib/api';
import { toDate } from '../lib/helpers';

class Log extends Component {
	state = {
		logs: []
	};

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const { logs } = await get('logs');
		this.setState({ logs });
	};

	render() {
		const { logs } = this.state;

		return (
			<section className="log">
				<article>
					<h1>Log</h1>
					<Card>
						<nav className="corner">
							<button className="button success small" type="button">
								Export
							</button>
						</nav>
						{logs.reverse().map(({ timestamp, level, message }) => (
							<p key={timestamp} className={level.toLowerCase()}>
								<strong>{toDate(timestamp, true)}</strong>
								<span>{message}</span>
							</p>
						))}
					</Card>
				</article>
			</section>
		);
	}
}

export default Log;
